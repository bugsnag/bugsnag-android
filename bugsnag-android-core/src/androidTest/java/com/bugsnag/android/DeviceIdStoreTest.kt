package com.bugsnag.android

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.bugsnag.android.internal.ImmutableConfig
import com.bugsnag.android.internal.dag.ValueProvider
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
    lateinit var fileInternal: File
    lateinit var storageDir: File
    lateinit var ctx: Context
    lateinit var prefMigrator: SharedPrefMigrator

    private val prefsId = "55670bff-9024-fc0a-b392-0242ac88ccd9"
    private val ids = listOf<String>(
        "ab0c1482-2ffe-11eb-adc1-0242ac120002",
        "d9901bff-2ffe-11eb-adc1-0242ac120002",
        "103f88a2-2ffe-11eb-adc1-0242ac120002",
        "8c55319d-2ffe-11eb-adc1-0242ac120002"
    )

    private fun uuidProvider(index: Int): () -> UUID {
        return { UUID.fromString(ids[index]) }
    }

    @Before
    fun setUp() {
        ctx = ApplicationProvider.getApplicationContext<Context>()
        prefMigrator = SharedPrefMigrator(ctx)
        storageDir = ctx.cacheDir
        file = File(storageDir, "device.json")
        file.delete()
        fileInternal = File(storageDir, "device_internal.json")
        fileInternal.delete()
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs =
            context.getSharedPreferences("com.bugsnag.android", Context.MODE_PRIVATE)
        prefs.edit().remove("install.iud").commit()
    }

    private fun generateConfig(generateAnonymousId: Boolean): ImmutableConfig {
        val config = BugsnagTestUtils.generateConfiguration()
        config.generateAnonymousId = generateAnonymousId
        return BugsnagTestUtils.convert(config)
    }

    /**
     * A file should be created if it does not already exist
     */
    @Test
    fun nonExistentFile() {
        val nonExistentFile = File(storageDir, "foo")
        nonExistentFile.delete()
        val nonExistentInternalFile = File(storageDir, "foo_internal")
        nonExistentInternalFile.delete()
        val store = DeviceIdStore(
            ctx,
            nonExistentFile,
            uuidProvider(0),
            nonExistentInternalFile,
            uuidProvider(1),
            sharedPrefMigrator = ValueProvider(prefMigrator),
            logger = NoopLogger,
            config = generateConfig(true)
        )

        val loaded = store.load()

        assertEquals(ids[0], loaded?.deviceId)
        assertEquals(ids[1], loaded?.internalDeviceId)
    }

    /**
     * An empty file should be overwritten with a new device ID
     */
    @Test
    fun emptyFile() {
        file.delete()
        assert(file.createNewFile())
        fileInternal.delete()
        assert(fileInternal.createNewFile())
        val store = DeviceIdStore(
            ctx,
            file,
            uuidProvider(0),
            fileInternal,
            uuidProvider(1),
            ValueProvider(prefMigrator),
            logger = NoopLogger,
            config = generateConfig(true)
        )

        val loaded = store.load()

        assertEquals(ids[0], loaded?.deviceId)
        assertEquals(ids[1], loaded?.internalDeviceId)
    }

    /**
     * A file of the correct length with invalid contents should be overwritten with a new device ID
     */
    @Test
    fun invalidFileContents() {
        file.writeText("{\"hamster\": 2}")
        fileInternal.writeText("{\"hamster\": 2}")
        val store = DeviceIdStore(
            ctx,
            file,
            uuidProvider(0),
            fileInternal,
            uuidProvider(1),
            ValueProvider(prefMigrator),
            logger = NoopLogger,
            config = generateConfig(true)
        )

        val loaded = store.load()

        assertEquals(ids[0], loaded?.deviceId)
        assertEquals(ids[1], loaded?.internalDeviceId)
    }

    /**
     * A valid file should not be overwritten with a new device ID
     */
    @Test
    fun validFileContents() {
        file.writeText("{\"id\": \"${ids[0]}\"}")
        fileInternal.writeText("{\"id\": \"${ids[1]}\"}")
        val storeB = DeviceIdStore(
            ctx,
            file,
            uuidProvider(2),
            fileInternal,
            uuidProvider(3),
            ValueProvider(prefMigrator),
            logger = NoopLogger,
            config = generateConfig(true)
        )
        val storeA = DeviceIdStore(
            ctx,
            file,
            uuidProvider(0),
            fileInternal,
            uuidProvider(1),
            ValueProvider(prefMigrator),
            logger = NoopLogger,
            config = generateConfig(true)
        )

        val loadedA = storeA.load()
        val loadedB = storeB.load()

        // device ID is loaded without writing file
        assertEquals(ids[0], loadedA?.deviceId)
        assertEquals(ids[1], loadedA?.internalDeviceId)

        // same device ID is retrieved as before
        assertEquals(ids[0], loadedB?.deviceId)
        assertEquals(ids[1], loadedB?.internalDeviceId)
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
        val nonReadableInternalFile = File(storageDir, "foo").apply {
            delete()
            createNewFile()
            setWritable(false)
        }
        val store = DeviceIdStore(
            ctx,
            nonReadableFile,
            uuidProvider(0),
            nonReadableInternalFile,
            uuidProvider(1),
            ValueProvider(prefMigrator),
            logger = NoopLogger,
            config = generateConfig(true)
        )

        val loaded = store.load()
        assertNull(loaded)
    }

    /**
     * The device ID store should take out a file lock to prevent concurrent writes to the file.
     */
    @Test(timeout = 2000)
    fun fileLockUsed() {
        val store = DeviceIdStore(
            ctx,
            file,
            uuidProvider(0),
            fileInternal,
            uuidProvider(1),
            ValueProvider(prefMigrator),
            logger = NoopLogger,
            config = generateConfig(true)
        )

        // load the device ID on many different background threads.
        // each thread races with each other, but only one should generate a device ID
        // and persist it to disk.
        val deviceIds = mutableSetOf<String>()
        val attempts = 16
        val executor = Executors.newFixedThreadPool(attempts)
        val latch = CountDownLatch(attempts)

        repeat(attempts) {
            executor.submit {
                store.load()?.deviceId?.let { deviceIds.add(it) }
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
        val store = DeviceIdStore(
            ctx,
            file,
            uuidProvider(0),
            fileInternal,
            uuidProvider(1),
            ValueProvider(prefMigrator),
            logger = NoopLogger,
            config = generateConfig(true)
        )
        val context = ApplicationProvider.getApplicationContext<Context>()

        val prefs =
            context.getSharedPreferences("com.bugsnag.android", Context.MODE_PRIVATE)
        prefs.edit().putString("install.iud", prefsId).commit()

        val prefDeviceId = prefMigrator.loadDeviceId(false)

        val loaded = store.load()

        val storeDeviceId = loaded?.deviceId
        assertEquals(prefsId, storeDeviceId)
        assertEquals(prefDeviceId, storeDeviceId)
    }

    /**
     * If generateAnonymousId is false, no device ID is returned (even if one is saved)
     */
    @Test
    fun loadWithoutGenerateAnonymousId() {
        file.writeText("{\"id\": \"${ids[0]}\"}")
        fileInternal.writeText("{\"id\": \"${ids[1]}\"}")
        val store = DeviceIdStore(
            ctx,
            file,
            uuidProvider(0),
            fileInternal,
            uuidProvider(1),
            sharedPrefMigrator = ValueProvider(prefMigrator),
            logger = NoopLogger,
            config = generateConfig(false)
        )

        val loaded = store.load()
        assertNull(loaded)
    }
}
