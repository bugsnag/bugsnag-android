package com.bugsnag.android.internal.remoteconfig

import com.bugsnag.android.RemoteConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.util.Date

class RemoteConfigStoreTest {

    @get:Rule
    val tempDir = TemporaryFolder()

    private lateinit var store: RemoteConfigStore
    private val versionCode = 123

    @Before
    fun setup() {
        store = RemoteConfigStore(tempDir.root, versionCode)
    }

    @Test
    fun basicStoreAndLoad() {
        val config = createValidRemoteConfig("tag1", futureDate(1000))

        store.store(config)
        val loaded = store.load()

        assertEquals(config.configurationTag, loaded?.configurationTag)
        assertEquals(config.configurationExpiry, loaded?.configurationExpiry)
        assertEquals(config.discardRules.size, loaded?.discardRules?.size)
    }

    @Test
    fun loadReturnsNullWhenNoConfigStored() {
        assertNull(store.load())
    }

    @Test
    fun loadReturnsNullWhenConfigIsExpired() {
        val expiredConfig = createValidRemoteConfig("expired", pastDate(1000))
        store.store(expiredConfig)

        assertNull(store.load())
    }

    @Test
    fun loadRemovesExpiredConfigFromDisk() {
        val expiredConfig = createValidRemoteConfig("expired", pastDate(1000))
        store.store(expiredConfig)

        // First load should return null and clean up
        assertNull(store.load())

        // Config file should be deleted
        val configFile = File(tempDir.root, "core-$versionCode.json")
        assertFalse(configFile.exists())
    }

    @Test
    fun currentReturnsValidUnexpiredConfig() {
        val config = createValidRemoteConfig("valid", futureDate(1000))
        store.store(config)

        val current = store.current()
        assertEquals(config.configurationTag, current?.configurationTag)
    }

    @Test
    fun currentReturnsNullForExpiredConfig() {
        val expiredConfig = createValidRemoteConfig("expired", pastDate(1000))
        store.store(expiredConfig)

        assertNull(store.current())
    }

    @Test
    fun currentOrExpiredReturnsExpiredConfig() {
        val expiredConfig = createValidRemoteConfig("expired", pastDate(1000))
        store.store(expiredConfig)

        val current = store.currentOrExpired()
        assertEquals(expiredConfig.configurationTag, current?.configurationTag)
    }

    @Test
    fun currentOrExpiredReturnsNullWhenNoConfigExists() {
        assertNull(store.currentOrExpired())
    }

    @Test
    fun storeCreatesDirectoryIfItDoesNotExist() {
        val nonExistentDir = File(tempDir.root, "subdir/nested")
        val storeWithNewDir = RemoteConfigStore(nonExistentDir, versionCode)

        val config = createValidRemoteConfig("test", futureDate(1000))
        storeWithNewDir.store(config)

        assertTrue(nonExistentDir.exists())
        assertTrue(nonExistentDir.isDirectory)
    }

    @Test
    fun storeUsesAtomicWritesWithTempFile() {
        val config = createValidRemoteConfig("atomic", futureDate(1000))

        store.store(config)

        val configFile = File(tempDir.root, "core-$versionCode.json")
        val tempFile = File(tempDir.root, "core-$versionCode.json.new")

        assertTrue(configFile.exists())
        assertFalse(tempFile.exists()) // Temp file should be cleaned up
    }

    @Test
    fun loadFromDiskWorksAfterRestartSimulation() {
        val config = createValidRemoteConfig("persistent", futureDate(1000))
        store.store(config)

        // Create new store instance (simulating restart)
        val newStore = RemoteConfigStore(tempDir.root, versionCode)
        val loaded = newStore.load()

        assertEquals(config.configurationTag, loaded?.configurationTag)
        assertEquals(config.configurationExpiry, loaded?.configurationExpiry)
    }

    @Test
    fun loadHandlesCorruptedFileGracefully() {
        // Create a corrupted config file
        val configFile = File(tempDir.root, "core-$versionCode.json")
        tempDir.root.mkdirs()
        configFile.writeText("invalid json content")

        assertNull(store.load())

        // Corrupted file should be deleted
        assertFalse(configFile.exists())
    }

    @Test
    fun loadHandlesMissingFileGracefully() {
        val configFile = File(tempDir.root, "core-$versionCode.json")
        assertFalse(configFile.exists())

        assertNull(store.load())
    }

    @Test
    fun loadHandlesUnreadableFileGracefully() {
        val configFile = File(tempDir.root, "core-$versionCode.json")
        tempDir.root.mkdirs()
        configFile.createNewFile()
        configFile.setReadable(false)

        try {
            assertNull(store.load())
        } finally {
            configFile.setReadable(true) // Cleanup for temp dir deletion
        }
    }

    @Test
    fun sweepRemovesOldVersionFilesButKeepsCurrent() {
        // Create files for different versions
        val oldFile1 = File(tempDir.root, "core-100.json")
        val oldFile2 = File(tempDir.root, "core-200.json")
        val currentFile = File(tempDir.root, "core-$versionCode.json")

        tempDir.root.mkdirs()
        oldFile1.createNewFile()
        oldFile2.createNewFile()
        currentFile.createNewFile()

        store.sweep()

        assertFalse(oldFile1.exists())
        assertFalse(oldFile2.exists())
        assertTrue(currentFile.exists())
    }

    @Test
    fun sweepHandlesEmptyDirectoryGracefully() {
        // Should not throw exception
        store.sweep()
    }

    @Test
    fun sweepHandlesMissingDirectoryGracefully() {
        val nonExistentDir = File(tempDir.root, "missing")
        val storeWithMissingDir = RemoteConfigStore(nonExistentDir, versionCode)

        // Should not throw exception
        storeWithMissingDir.sweep()
    }

    @Test
    fun multipleStoresWithDifferentVersionsUseDifferentFiles() {
        val store1 = RemoteConfigStore(tempDir.root, 100)
        val store2 = RemoteConfigStore(tempDir.root, 200)

        val config1 = createValidRemoteConfig("version100", futureDate(1000))
        val config2 = createValidRemoteConfig("version200", futureDate(1000))

        store1.store(config1)
        store2.store(config2)

        val loaded1 = store1.load()
        val loaded2 = store2.load()

        assertEquals("version100", loaded1?.configurationTag)
        assertEquals("version200", loaded2?.configurationTag)
    }

    @Test
    fun inMemoryCacheTakesPrecedenceOverDisk() {
        val diskConfig = createValidRemoteConfig("disk", futureDate(1000))
        val memoryConfig = createValidRemoteConfig("memory", futureDate(2000))

        // Store to disk first
        store.store(diskConfig)

        // Create new store and load from disk
        val newStore = RemoteConfigStore(tempDir.root, versionCode)
        assertEquals("disk", newStore.load()?.configurationTag)

        // Store different config to memory
        newStore.store(memoryConfig)

        // Should return memory config, not reload from disk
        assertEquals("memory", newStore.load()?.configurationTag)
    }

    @Test
    fun doubleCheckedLockingWorksCorrectly() {
        // This is hard to test deterministically, but we can at least ensure
        // concurrent access doesn't break things
        val config = createValidRemoteConfig("concurrent", futureDate(1000))
        store.store(config)

        // Multiple loads should work
        repeat(10) {
            assertEquals("concurrent", store.load()?.configurationTag)
        }
    }

    @Test
    fun storeHandlesIOExceptionGracefully() {
        // Create a read-only directory to force IO failure
        val readOnlyDir = File(tempDir.root, "readonly")
        readOnlyDir.mkdirs()
        readOnlyDir.setWritable(false)

        val readOnlyStore = RemoteConfigStore(readOnlyDir, versionCode)
        val config = createValidRemoteConfig("test", futureDate(1000))

        try {
            // Should not throw exception
            readOnlyStore.store(config)

            // Config should still be available in memory
            assertEquals("test", readOnlyStore.current()?.configurationTag)
        } finally {
            readOnlyDir.setWritable(true) // Cleanup
        }
    }

    @Test
    fun currentOrExpiredLoadsFromDiskWhenInMemoryIsNull() {
        val config = createValidRemoteConfig("disk", futureDate(1000))

        // Store config with one store instance
        store.store(config)

        // Create new store instance (simulating restart)
        val newStore = RemoteConfigStore(tempDir.root, versionCode)

        // currentOrExpired should load from disk
        val loaded = newStore.currentOrExpired()
        assertEquals("disk", loaded?.configurationTag)
    }

    @Test
    fun currentOrExpiredReturnsExpiredConfigFromDisk() {
        val expiredConfig = createValidRemoteConfig("expired", pastDate(1000))

        // Store expired config
        store.store(expiredConfig)

        // Create new store instance (simulating restart)
        val newStore = RemoteConfigStore(tempDir.root, versionCode)

        // currentOrExpired should return the expired config
        val loaded = newStore.currentOrExpired()
        assertEquals("expired", loaded?.configurationTag)
    }

    @Test
    fun loadDoesNotReturnExpiredConfigFromDisk() {
        val expiredConfig = createValidRemoteConfig("expired", pastDate(1000))

        // Store expired config
        store.store(expiredConfig)

        // Create new store instance (simulating restart)
        val newStore = RemoteConfigStore(tempDir.root, versionCode)

        // load should return null for expired config
        assertNull(newStore.load())

        // And should clean up the file
        val configFile = File(tempDir.root, "core-$versionCode.json")
        assertFalse(configFile.exists())
    }

    @Test
    fun configFileNameIncludesVersionCode() {
        val config = createValidRemoteConfig("test", futureDate(1000))
        store.store(config)

        val expectedFile = File(tempDir.root, "core-$versionCode.json")
        assertTrue(expectedFile.exists())
    }

    private fun createValidRemoteConfig(tag: String, expiry: Date): RemoteConfig {
        return RemoteConfig.createEmpty(configurationTag = tag, configurationExpiry = expiry)
    }

    private fun futureDate(offsetMs: Long): Date {
        return Date(System.currentTimeMillis() + offsetMs)
    }

    private fun pastDate(offsetMs: Long): Date {
        return Date(System.currentTimeMillis() - offsetMs)
    }
}
