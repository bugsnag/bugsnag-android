package com.bugsnag.android

import android.content.Context
import android.util.JsonReader
import java.io.File
import java.io.IOException
import java.lang.Thread
import java.nio.channels.FileChannel
import java.nio.channels.FileLock
import java.nio.channels.OverlappingFileLockException
import java.util.UUID

/**
 * This class is responsible for persisting and retrieving the device ID which uniquely
 * identifies this device.
 *
 * This class is made multi-process safe through the use of a [FileLock], and thread safe
 * through the use of a [ReadWriteLock] in [SynchronizedStreamableStore].
 */
internal class DeviceIdStore @JvmOverloads constructor(
    context: Context,
    private val file: File = File(context.filesDir, "device-id"),
    private val sharedPrefMigrator: SharedPrefMigrator,
    private val logger: Logger
) {

    private val synchronizedStreamableStore: SynchronizedStreamableStore<DeviceId>

    init {
        try {
            if (!file.exists()) {
                file.createNewFile()
            }
        } catch (exc: Throwable) {
            logger.w("Failed to created device ID file", exc)
        }
        this.synchronizedStreamableStore = SynchronizedStreamableStore(file)
    }

    /**
     * Loads the device ID from its file system location. Device IDs are UUIDs which are
     * persisted on a per-install basis. This method is thread-safe and multi-process safe.
     *
     * If no device ID exists then the legacy value stored in [SharedPreferences] will
     * be used. If no value is present then a random UUID will be generated and persisted.
     */
    fun loadDeviceId(): String? {
        return loadDeviceId {
            when (val legacyDeviceId = sharedPrefMigrator.loadDeviceId()) {
                null -> UUID.randomUUID()
                else -> UUID.fromString(legacyDeviceId)
            }
        }
    }

    internal fun loadDeviceId(uuidProvider: () -> UUID): String? {
        return try {
            // optimistically read device ID without a lock - the majority of the time
            // the device ID will already be present so no synchronization is required.
            val deviceId = loadDeviceIdInternal()

            if (deviceId?.id != null) {
                deviceId.id
            } else {
                return persistNewDeviceUuid(uuidProvider)
            }
        } catch (exc: Throwable) {
            logger.w("Failed to load device ID", exc)
            null
        }
    }

    /**
     * Loads the device ID from the file.
     *
     * If the file has zero length it can't contain device ID, so reading will be skipped.
     */
    private fun loadDeviceIdInternal(): DeviceId? {
        if (file.length() > 0) {
            try {
                return synchronizedStreamableStore.load(DeviceId.Companion::fromReader)
            } catch (exc: Throwable) { // catch AssertionError which can be thrown by JsonReader
                // on Android 8.0/8.1. see https://issuetracker.google.com/issues/79920590
                logger.w("Failed to load device ID", exc)
            }
        }
        return null
    }

    /**
     * Write a new Device ID to the file.
     */
    private fun persistNewDeviceUuid(uuidProvider: () -> UUID): String? {
        return try {
            // acquire a FileLock to prevent Clients in different processes writing
            // to the same file concurrently
            file.outputStream().channel.use { channel ->
                persistNewDeviceIdWithLock(channel, uuidProvider)
            }
        } catch (exc: IOException) {
            logger.w("Failed to persist device ID", exc)
            null
        }
    }

    private fun persistNewDeviceIdWithLock(
        channel: FileChannel,
        uuidProvider: () -> UUID
    ): String? {
        val lock = waitForFileLock(channel) ?: return null

        return try {
            // read the device ID again as it could have changed
            // between the last read and when the lock was acquired
            val deviceId = loadDeviceIdInternal()

            if (deviceId?.id != null) {
                // the device ID changed between the last read
                // and acquiring the lock, so return the generated value
                deviceId.id
            } else {
                // generate a new device ID and persist it
                val newId = DeviceId(uuidProvider().toString())
                synchronizedStreamableStore.persist(newId)
                newId.id
            }
        } finally {
            lock.release()
        }
    }

    /**
     * Attempt to acquire a file lock. If [OverlappingFileLockException] is thrown
     * then the method will wait for 50ms then try again, for a maximum of 10 attempts.
     */
    private fun waitForFileLock(channel: FileChannel): FileLock? {
        repeat(MAX_FILE_LOCK_ATTEMPTS) {
            try {
                return channel.tryLock()
            } catch (exc: OverlappingFileLockException) {
                Thread.sleep(FILE_LOCK_WAIT_MS)
            }
        }
        return null
    }

    companion object {
        private const val MAX_FILE_LOCK_ATTEMPTS = 20
        private const val FILE_LOCK_WAIT_MS = 25L
    }
}

/**
 * Serializes and deserializes the device ID to/from JSON.
 */
private class DeviceId(val id: String?) : JsonStream.Streamable {

    override fun toStream(stream: JsonStream) {
        with(stream) {
            beginObject()
            name(KEY_ID)
            value(id)
            endObject()
        }
    }

    companion object : JsonReadable<DeviceId> {
        private const val KEY_ID = "id"

        override fun fromReader(reader: JsonReader): DeviceId {
            var id: String? = null
            with(reader) {
                beginObject()
                if (hasNext() && KEY_ID == nextName()) {
                    id = nextString()
                }
            }
            return DeviceId(id)
        }
    }
}
