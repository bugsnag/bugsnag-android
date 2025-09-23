package com.bugsnag.android.internal

import com.bugsnag.android.RemoteConfig
import java.io.File
import java.io.IOException
import java.util.Date
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

internal class RemoteConfigStore(
    val configDir: File,
    val appVersionCode: Int,
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

    fun current(): RemoteConfig? {
        // First check in-memory config
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
            return memoryConfig
        }

        // Load from disk if in-memory config is null
        lock.withLock {
            // Double-check after acquiring lock
            val recheck = current
            if (recheck != null) {
                return recheck
            }

            val diskConfig = loadFromDisk()
            if (diskConfig != null) {
                current = diskConfig
                return diskConfig
            }
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
            return memoryConfig
        }

        // Load from disk if in-memory config is null or expired
        lock.withLock {
            // Double-check after acquiring lock
            val recheck = current
            if (recheck != null && !isExpired(recheck)) {
                return recheck
            }

            val diskConfig = loadFromDisk()
            if (diskConfig != null && !isExpired(diskConfig)) {
                current = diskConfig
                return diskConfig
            }

            // Clear expired config
            if (diskConfig != null && isExpired(diskConfig)) {
                current = null
                deleteConfigFile()
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
                configDir.mkdirs()

                // Write to temporary file first using JsonHelper
                JsonHelper.serialize(remoteConfig, tempFile)

                // Atomically move temp file to final location
                if (!tempFile.renameTo(configFile)) {
                    // If rename fails, try to delete the old file and rename again
                    configFile.delete()
                    tempFile.renameTo(configFile)
                }
            } catch (_: IOException) {
                // If disk write fails, at least keep the in-memory version
                // Log could be added here if logger is available
            }
        }
    }

    private fun loadFromDisk(): RemoteConfig? {
        val configFile = File(configDir, configFileName())
        if (!configFile.exists() || !configFile.canRead()) {
            return null
        }

        return try {
            configFile.inputStream().use { inputStream ->
                val map = JsonHelper.deserialize(inputStream)
                RemoteConfig.fromMap(map)
            }
        } catch (_: Exception) {
            // If parsing fails, delete the corrupted file
            configFile.delete()
            null
        }
    }

    private fun isExpired(remoteConfig: RemoteConfig): Boolean {
        return remoteConfig.configurationExpiry.before(Date())
    }

    private fun deleteConfigFile() {
        val configFile = File(configDir, configFileName())
        if (configFile.exists()) {
            configFile.delete()
        }
    }

    private fun configFileName(): String = "core-${appVersionCode}.json"
}
