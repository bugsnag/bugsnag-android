package com.bugsnag.android.internal.remoteconfig

import com.bugsnag.android.Logger
import com.bugsnag.android.NoopLogger
import com.bugsnag.android.RemoteConfig
import com.bugsnag.android.internal.JsonHelper
import java.io.File
import java.io.IOException
import java.util.Date
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

internal class RemoteConfigStore(
    val configDir: File,
    val appVersionCode: Int,
    private val logger: Logger = NoopLogger,
) {

    private val lock = ReentrantLock()

    @Volatile
    private var current: RemoteConfig? = null

    fun sweep() {
        lock.withLock {
            val currentFilename = configFileName()
            val files = configDir.listFiles()
            if (files != null) {
                for (file in files) {
                    if (file.name != currentFilename) {
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
            logger.d("Remote config already loaded in memory; returning cached config ${describeRemoteConfig(memoryConfig)}")
            return memoryConfig
        }

        // Load from disk if in-memory config is null
        lock.withLock {
            // Double-check after acquiring lock
            val recheck = current
            if (recheck != null) {
                logger.d("Remote config became available in memory while waiting for the lock; returning ${describeRemoteConfig(recheck)}")
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

    fun clear() {
        lock.withLock {
            logger.d("Clearing remote config from memory and disk")
            current = null
            deleteConfigFiles()
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
                    logger.d("Parsed remote config from disk at ${configFile.absolutePath} ${describeRemoteConfig(remoteConfig)}")
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

    private fun configFileName(): String = "core-$appVersionCode.json"

    private fun describeRemoteConfig(remoteConfig: RemoteConfig): String {
        return "tag=${remoteConfig.configurationTag ?: "<null>"}, expiry=${remoteConfig.configurationExpiry.time}, discardRules=${remoteConfig.discardRules.size}"
    }
}
