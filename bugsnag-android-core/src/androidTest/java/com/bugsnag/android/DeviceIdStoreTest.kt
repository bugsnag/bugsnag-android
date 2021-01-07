package com.bugsnag.android

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import junit.framework.TestCase.assertNull
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.io.File
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors

internal class DeviceIdStoreTest {

    lateinit var file: File
    lateinit var storageDir: File
    lateinit var ctx: Context
    lateinit var prefMigrator: SharedPrefMigrator

    private val uuidProvider = {
        UUID.fromString("ab0c1482-2ffe-11eb-adc1-0242ac120002")
    }
    private val diffUuidProvider = {
        UUID.fromString("d9901bff-2ffe-11eb-adc1-0242ac120002")
    }

    @Before
    fun setUp() {
        ctx = ApplicationProvider.getApplicationContext<Context>()
        prefMigrator = SharedPrefMigrator(ctx)
        storageDir = ctx.cacheDir
        file = File(storageDir, "device.json")
        file.delete()
    }

    /**
     * A file should be created if it does not already exist
     */
    @Test
    fun nonExistentFile() {
        val nonExistentFile = File(storageDir, "foo")
        nonExistentFile.delete()
        val store = DeviceIdStore(ctx, nonExistentFile, prefMigrator, NoopLogger)
        val deviceId = store.loadDeviceId(uuidProvider)
        requireNotNull(deviceId)
        assertEquals("ab0c1482-2ffe-11eb-adc1-0242ac120002", deviceId)
    }

    /**
     * An empty file should be overwritten with a new device ID
     */
    @Test
    fun emptyFile() {
        val store = DeviceIdStore(ctx, file, prefMigrator, NoopLogger)
        val deviceId = store.loadDeviceId(uuidProvider)
        requireNotNull(deviceId)
        assertEquals("ab0c1482-2ffe-11eb-adc1-0242ac120002", deviceId)
    }

    /**
     * A file of the incorrect length should be overwritten with a new device ID
     */
    @Test
    fun incorrectFileLength() {
        val store = DeviceIdStore(ctx, file, prefMigrator, NoopLogger)
        val deviceId = store.loadDeviceId(uuidProvider)
        requireNotNull(deviceId)
        assertEquals("ab0c1482-2ffe-11eb-adc1-0242ac120002", deviceId)
    }

    /**
     * A file of the correct length with invalid contents should be overwritten with a new device ID
     */
    @Test
    fun invalidFileContents() {
        file.writeText("{\"hamster\": 2}")
        val store = DeviceIdStore(ctx, file, prefMigrator, NoopLogger)
        val deviceId = store.loadDeviceId(uuidProvider)
        requireNotNull(deviceId)
        assertEquals("ab0c1482-2ffe-11eb-adc1-0242ac120002", deviceId)
    }

    /**
     * A valid file should not be overwritten with a new device ID
     */
    @Test
    fun validFileContents() {
        file.writeText("{\"id\": \"24c51482-2ffe-11eb-adc1-0242ac120002\"}")
        val store = DeviceIdStore(ctx, file, prefMigrator, NoopLogger)

        // device ID is loaded without writing file
        assertEquals(
            "24c51482-2ffe-11eb-adc1-0242ac120002",
            store.loadDeviceId(uuidProvider)
        )
        // same device ID is retrieved as before
        assertEquals(
            "24c51482-2ffe-11eb-adc1-0242ac120002",
            store.loadDeviceId(diffUuidProvider)
        )
    }

    /**
     * A non-writable file does not crash the app
     */
    @Test
    fun nonWritableFile() {
        val nonReadableFile = File(storageDir, "foo").apply {
            delete()
            createNewFile()
            setWritable(false)
        }
        val store = DeviceIdStore(ctx, nonReadableFile, prefMigrator, NoopLogger)
        val deviceId = store.loadDeviceId(uuidProvider)
        assertNull(deviceId)
    }

    /**
     * The device ID store should take out a file lock to prevent concurrent writes to the file.
     */
    @Test(timeout = 2000)
    fun fileLockUsed() {
        val store = DeviceIdStore(ctx, file, prefMigrator, NoopLogger)

        // load the device ID on many different background threads.
        // each thread races with each other, but only one should generate a device ID
        // and persist it to disk.
        val deviceIds = mutableSetOf<String>()
        val attempts = 16
        val executor = Executors.newFixedThreadPool(attempts)
        val latch = CountDownLatch(attempts)

        repeat(attempts) {
            executor.submit {
                val id = store.loadDeviceId(uuidProvider)
                requireNotNull(id)
                deviceIds.add(id)
                latch.countDown()
            }
        }
        latch.await()

        // validate that the device ID is consistent for each.
        // the device ID of whichever thread won the race first should be used.
        assertEquals(1, deviceIds.size)
    }

    /**
     * The device ID store should migrate legacy IDs from shared preferences if they are present
     */
    @Test
    fun sharedPrefMigration() {
        val store = DeviceIdStore(ctx, file, prefMigrator, NoopLogger)
        val context = ApplicationProvider.getApplicationContext<Context>()

        val prefs =
            context.getSharedPreferences("com.bugsnag.android", Context.MODE_PRIVATE)
        prefs.edit().putString("install.iud", "55670bff-9024-fc0a-b392-0242ac88ccd9").commit()

        val prefDeviceId = SharedPrefMigrator(context).loadDeviceId()
        val storeDeviceId = store.loadDeviceId()
        assertEquals("55670bff-9024-fc0a-b392-0242ac88ccd9", storeDeviceId)
        assertEquals(prefDeviceId, storeDeviceId)
    }
}
