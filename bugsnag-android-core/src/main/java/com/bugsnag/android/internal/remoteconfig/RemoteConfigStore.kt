package com.bugsnag.android.internal.remoteconfig

import com.bugsnag.android.Logger
import com.bugsnag.android.NoopLogger
import com.bugsnag.android.RemoteConfig
import com.bugsnag.android.internal.JsonHelper
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.channels.OverlappingFileLockException
import java.util.Date
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

@Suppress("TooManyFunctions")
internal class RemoteConfigStore(
    val configDir: File,
    val appVersionCode: Int,
    private val logger: Logger = NoopLogger,
) {

    private val lock = ReentrantLock()

    @Volatile
    private var current: RemoteConfig? = null

    @Volatile
    private var cachedCooldownUntil: Long = 0L

    fun sweep() {
        lock.withLock {
            val currentFilename = configFileName()
            val currentCooldownFilename = cooldownFile().name
            val currentLockFilename = lockFile().name
            val files = configDir.listFiles()
            if (files != null) {
                for (file in files) {
                    if (
                        file.name != currentFilename &&
                        file.name != currentCooldownFilename &&
                        file.name != currentLockFilename
                    ) {
                        file.delete()
                    }
                }
            }
        }
    }

    /**
     * Returns the current in-memory config if it is still valid (or `null` if it has expired).
     */
    fun current(): RemoteConfig? {
        val memoryConfig = current
        if (memoryConfig != null && !isExpired(memoryConfig)) {
            return memoryConfig
        }

        return null
    }

    /**
     * Returns the current RemoteConfig if is has been loaded, but return the last known "good"
     * config if there isn't a valid "current".
     */
    fun currentOrExpired(): RemoteConfig? {
        val memoryConfig = current
        if (memoryConfig != null) {
            logger.d(
                "Remote config already loaded in memory; returning cached config " +
                    describeRemoteConfig(memoryConfig)
            )
            return memoryConfig
        }

        // Load from disk if in-memory config is null
        lock.withLock {
            // Double-check after acquiring lock
            val recheck = current
            if (recheck != null) {
                logger.d(
                    "Remote config became available in memory while waiting for the lock; " +
                        "returning ${describeRemoteConfig(recheck)}"
                )
                return recheck
            }

            val diskConfig = loadFromDisk()
            if (diskConfig != null) {
                current = diskConfig
                logger.d("Loaded remote config from disk for currentOrExpired() ${describeRemoteConfig(diskConfig)}")
                return diskConfig
            }

            logger.d("No remote config available in memory or on disk for currentOrExpired()")
        }

        return null
    }

    /**
     * Reloads the disk cache even when this process has an in-memory value. This is used after
     * acquiring the cross-process request lock, when another process may have refreshed it.
     */
    fun reloadCurrentOrExpired(): RemoteConfig? = lock.withLock {
        val diskConfig = loadFromDisk()
        if (diskConfig != null) {
            current = diskConfig
            logger.d("Reloaded remote config from disk after cross-process lock ${describeRemoteConfig(diskConfig)}")
            diskConfig
        } else {
            current
        }
    }

    /**
     * Loads the RemoteConfig, favouring the in-memory version over loading from disk.
     * Always validates the expiry time before returning.
     */
    fun load(): RemoteConfig? {
        val memoryConfig = current()
        if (memoryConfig != null) {
            logger.d("Loaded remote config from memory ${describeRemoteConfig(memoryConfig)}")
            return memoryConfig
        }

        // Load from disk if in-memory config is null or expired
        lock.withLock {
            // Double-check after acquiring lock
            val recheck = current()
            if (recheck != null) {
                logger.d("Loaded remote config from memory after lock recheck ${describeRemoteConfig(recheck)}")
                return recheck
            }

            val diskConfig = loadFromDisk()
            if (diskConfig != null) {
                current = diskConfig
                if (!isExpired(diskConfig)) {
                    logger.d("Loaded remote config from disk into memory ${describeRemoteConfig(diskConfig)}")
                    return diskConfig
                } else {
                    logger.d("Loaded expired remote config from disk into memory ${describeRemoteConfig(diskConfig)}")
                }
            } else {
                logger.d("No remote config file was available to load from disk")
            }
        }

        return null
    }

    /**
     * Atomically stores the RemoteConfig both in memory and to disk.
     */
    fun store(remoteConfig: RemoteConfig) {
        lock.withLock {
            // Update in-memory first
            current = remoteConfig

            // Then persist to disk atomically
            try {
                val configFile = File(configDir, configFileName())
                val tempFile = File(configDir, "${configFileName()}.new")

                // Ensure config directory exists
                if (!configDir.exists() && !configDir.mkdirs() && !configDir.exists()) {
                    logger.w("Failed to create remote config directory at ${configDir.absolutePath}")
                }

                // Write to temporary file first using JsonHelper
                JsonHelper.serialize(remoteConfig, tempFile)

                // Atomically move temp file to final location
                var stored = tempFile.renameTo(configFile)
                if (!stored) {
                    // If rename fails, try to delete the old file and rename again
                    configFile.delete()
                    stored = tempFile.renameTo(configFile)
                }

                if (stored) {
                    logger.i(
                        "Stored remote config on disk at ${configFile.absolutePath} " +
                            "${describeRemoteConfig(remoteConfig)} fileExists=${configFile.exists()}"
                    )
                } else {
                    logger.w(
                        "Failed to move remote config into place at ${configFile.absolutePath} " +
                            "tempExists=${tempFile.exists()}"
                    )
                }
            } catch (ex: IOException) {
                // If disk write fails, at least keep the in-memory version
                logger.w("Failed to persist remote config to disk at ${configDir.absolutePath}/${configFileName()}", ex)
            }
        }
    }

    fun cooldownUntil(): Long = lock.withLock {
        if (cachedCooldownUntil > System.currentTimeMillis()) {
            return@withLock cachedCooldownUntil
        }

        readCooldownUntil()
    }

    /** Reloads the marker from disk after acquiring the cross-process request lock. */
    fun reloadCooldownUntil(): Long = lock.withLock {
        cachedCooldownUntil = 0L
        readCooldownUntil()
    }

    /**
     * Runs [block] while holding the per-version lock shared by all app processes. The lock wait
     * is bounded so an abandoned or slow peer cannot block event delivery indefinitely.
     */
    @Suppress("NestedBlockDepth", "ReturnCount")
    fun <T> withCrossProcessLock(block: () -> T): T? {
        try {
            if (!configDir.exists() && !configDir.mkdirs() && !configDir.exists()) {
                logger.w("Failed to create remote config directory at ${configDir.absolutePath} for cross-process lock")
                return null
            }

            RandomAccessFile(lockFile(), "rw").channel.use { channel ->
                val deadline = System.currentTimeMillis() + CROSS_PROCESS_LOCK_WAIT_MS
                var fileLock = tryAcquireLock(channel)
                while (fileLock == null && System.currentTimeMillis() < deadline) {
                    try {
                        Thread.sleep(CROSS_PROCESS_LOCK_RETRY_MS)
                    } catch (ex: InterruptedException) {
                        Thread.currentThread().interrupt()
                        logger.w("Interrupted while waiting for remote config cross-process lock", ex)
                        return null
                    }
                    fileLock = tryAcquireLock(channel)
                }

                if (fileLock == null) {
                    logger.w("Timed out waiting for remote config cross-process lock")
                    return null
                }

                try {
                    logger.d("Acquired remote config cross-process lock")
                    return block()
                } finally {
                    fileLock.release()
                    logger.d("Released remote config cross-process lock")
                }
            }
        } catch (ex: IOException) {
            logger.w("Failed to acquire remote config cross-process lock", ex)
            return null
        }
    }

    private fun readCooldownUntil(): Long {
        val cooldownFile = cooldownFile()
        if (!cooldownFile.exists()) {
            return 0L
        }

        return cooldownFile.readText().trim().toLongOrNull()?.also {
            cachedCooldownUntil = it
            logger.d("Loaded remote config cooldown marker until=$it file=${cooldownFile.absolutePath}")
        } ?: run {
            logger.w("Invalid remote config cooldown marker at ${cooldownFile.absolutePath}; deleting it")
            cooldownFile.delete()
            0L
        }
    }

    fun setCooldownUntil(timestamp: Long) = lock.withLock {
        cachedCooldownUntil = timestamp
        try {
            if (!configDir.exists() && !configDir.mkdirs() && !configDir.exists()) {
                logger.w("Failed to create remote config directory at ${configDir.absolutePath} for cooldown marker")
                return@withLock
            }
            cooldownFile().writeText(timestamp.toString())
            logger.i("Stored remote config cooldown marker until=$timestamp file=${cooldownFile().absolutePath}")
        } catch (ex: IOException) {
            logger.w("Failed to persist remote config cooldown marker", ex)
        }
    }

    fun clearCooldown() = lock.withLock {
        cachedCooldownUntil = 0L
        val cooldownFile = cooldownFile()
        if (cooldownFile.exists()) {
            cooldownFile.delete()
            logger.d("Cleared remote config cooldown marker file=${cooldownFile.absolutePath}")
        }
    }

    fun diagnostics(): String = lock.withLock {
        val configFile = File(configDir, configFileName())
        "memory=${current?.let(::describeRemoteConfig) ?: "<none>"}, " +
            "diskExists=${configFile.exists()}, diskReadable=${configFile.canRead()}, " +
            "cooldownUntil=${cooldownUntil()}"
    }

    fun clear() {
        lock.withLock {
            logger.d("Clearing remote config from memory and disk")
            current = null
            deleteConfigFiles()
            clearCooldown()
        }
    }

    private fun loadFromDisk(): RemoteConfig? {
        val configFile = File(configDir, configFileName())
        if (!configFile.exists()) {
            logger.d("Remote config file does not exist on disk at ${configFile.absolutePath}")
            return null
        }

        if (!configFile.canRead()) {
            logger.w("Remote config file exists but is not readable at ${configFile.absolutePath}")
            return null
        }

        return try {
            configFile.inputStream().use { inputStream ->
                val map = JsonHelper.deserialize(inputStream)
                val remoteConfig = RemoteConfig.fromJsonMap(map)
                if (remoteConfig != null) {
                    logger.d(
                        "Parsed remote config from disk at ${configFile.absolutePath} " +
                            describeRemoteConfig(remoteConfig)
                    )
                } else {
                    logger.w("Remote config file at ${configFile.absolutePath} did not contain a valid config")
                }
                remoteConfig
            }
        } catch (ex: Exception) {
            // If parsing fails, delete the corrupted file
            logger.w("Failed to parse remote config file at ${configFile.absolutePath}; deleting corrupted file", ex)
            configFile.delete()
            null
        }
    }

    private fun isExpired(remoteConfig: RemoteConfig): Boolean {
        return remoteConfig.configurationExpiry.before(Date())
    }

    private fun deleteConfigFiles() {
        val configFile = File(configDir, configFileName())
        val tempFile = File(configDir, "${configFileName()}.new")
        configFile.delete()
        tempFile.delete()
        logger.d(
            "Deleted remote config files configExists=${configFile.exists()} tempExists=${tempFile.exists()}"
        )
    }

    private fun cooldownFile(): File = File(configDir, "${configFileName()}.cooldown")

    private fun lockFile(): File = File(configDir, "${configFileName()}.lock")

    private fun tryAcquireLock(channel: java.nio.channels.FileChannel) = try {
        channel.tryLock()
    } catch (_: OverlappingFileLockException) {
        null
    }

    private fun configFileName(): String = "core-$appVersionCode.json"

    private fun describeRemoteConfig(remoteConfig: RemoteConfig): String {
        return "tag=${remoteConfig.configurationTag ?: "<null>"}, " +
            "expiry=${remoteConfig.configurationExpiry.time}, " +
            "discardRules=${remoteConfig.discardRules.size}"
    }

    private companion object {
        const val CROSS_PROCESS_LOCK_WAIT_MS = 5_000L
        const val CROSS_PROCESS_LOCK_RETRY_MS = 50L
    }
}
