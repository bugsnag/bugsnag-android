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
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

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
    fun clearRemovesInMemoryAndDiskState() {
        val config = createValidRemoteConfig("tag1", futureDate(1000))

        store.store(config)
        store.setCooldownUntil(System.currentTimeMillis() + 60_000)
        store.clear()

        assertNull(store.current())
        assertNull(store.load())

        val configFile = File(tempDir.root, "core-$versionCode.json")
        val tempFile = File(tempDir.root, "core-$versionCode.json.new")
        assertFalse(configFile.exists())
        assertFalse(tempFile.exists())
        assertEquals(0L, store.cooldownUntil())
    }

    @Test
    fun cooldownMarkerPersistsAndIsCleared() {
        val cooldownUntil = System.currentTimeMillis() + 60_000
        store.setCooldownUntil(cooldownUntil)

        assertEquals(cooldownUntil, store.cooldownUntil())

        val reloadedStore = RemoteConfigStore(tempDir.root, versionCode)
        assertEquals(cooldownUntil, reloadedStore.cooldownUntil())

        reloadedStore.clearCooldown()
        assertEquals(0L, reloadedStore.cooldownUntil())
    }

    @Test
    fun cooldownMarkerInSeparateDirSurvivesClearingTheConfigDir() {
        // Android may clear the cache directory (where the config lives) under storage pressure
        val configDir = tempDir.newFolder("cache", "config")
        val cooldownDir = tempDir.newFolder("files", "remote-config")
        val cooldownUntil = System.currentTimeMillis() + 60_000
        RemoteConfigStore(configDir, versionCode, cooldownDir = cooldownDir).apply {
            store(createValidRemoteConfig("tag1", futureDate(60_000)))
            setCooldownUntil(cooldownUntil, 2)
        }

        assertTrue(File(cooldownDir, "core-$versionCode.json.cooldown").exists())
        assertFalse(File(configDir, "core-$versionCode.json.cooldown").exists())

        configDir.deleteRecursively()
        val reloaded = RemoteConfigStore(configDir, versionCode, cooldownDir = cooldownDir)

        assertNull(reloaded.currentOrExpired())
        assertEquals(cooldownUntil, reloaded.cooldownUntil())
        assertEquals(2, reloaded.failureCount())
    }

    @Test
    fun legacyMarkerNextToTheConfigIsMovedToTheCooldownDir() {
        val configDir = tempDir.newFolder("cache", "config")
        val cooldownDir = File(tempDir.root, "files/remote-config") // created on demand
        val cooldownUntil = System.currentTimeMillis() + 60_000
        RemoteConfigStore(configDir, versionCode).setCooldownUntil(cooldownUntil, 3) // older SDK layout

        val upgraded = RemoteConfigStore(configDir, versionCode, cooldownDir = cooldownDir)

        assertEquals(cooldownUntil, upgraded.cooldownUntil())
        assertEquals(3, upgraded.failureCount())
        assertTrue(File(cooldownDir, "core-$versionCode.json.cooldown").exists())
        assertFalse(File(configDir, "core-$versionCode.json.cooldown").exists())
    }

    @Test
    fun sweepAndClearCoverTheCooldownDir() {
        val configDir = tempDir.newFolder("cache", "config")
        val cooldownDir = tempDir.newFolder("files", "remote-config")
        val staleMarker = File(cooldownDir, "core-${versionCode - 1}.json.cooldown").apply { writeText("1\n4") }
        val store = RemoteConfigStore(configDir, versionCode, cooldownDir = cooldownDir)
        store.setCooldownUntil(System.currentTimeMillis() + 60_000, 1)

        store.sweep()
        assertFalse("other versions' markers are swept", staleMarker.exists())
        assertTrue(File(cooldownDir, "core-$versionCode.json.cooldown").exists())

        store.clear()
        assertFalse(File(cooldownDir, "core-$versionCode.json.cooldown").exists())
        assertEquals(0L, RemoteConfigStore(configDir, versionCode, cooldownDir = cooldownDir).cooldownUntil())
    }

    @Test
    fun failureCountPersistsAcrossRestartsAndIsClearedOnSuccess() {
        val cooldownUntil = System.currentTimeMillis() + 60_000
        store.setCooldownUntil(cooldownUntil, 3)
        assertEquals(3, store.failureCount())

        val reloadedStore = RemoteConfigStore(tempDir.root, versionCode)
        assertEquals(cooldownUntil, reloadedStore.cooldownUntil())
        assertEquals(3, reloadedStore.failureCount())

        reloadedStore.clearCooldown()
        assertEquals(0, reloadedStore.failureCount())
        assertEquals(0, RemoteConfigStore(tempDir.root, versionCode).failureCount())
    }

    @Test
    fun failureCountSurvivesExpiredCooldown() {
        store.setCooldownUntil(System.currentTimeMillis() - 1, 2)

        val reloadedStore = RemoteConfigStore(tempDir.root, versionCode)
        assertEquals(2, reloadedStore.failureCount())
        reloadedStore.reloadCooldownUntil()
        assertEquals(2, reloadedStore.failureCount())
    }

    @Test
    fun legacyCooldownMarkerImpliesOneFailure() {
        val cooldownUntil = System.currentTimeMillis() + 60_000
        File(tempDir.root, "core-$versionCode.json.cooldown").writeText(cooldownUntil.toString())

        val reloadedStore = RemoteConfigStore(tempDir.root, versionCode)
        assertEquals(cooldownUntil, reloadedStore.cooldownUntil())
        assertEquals(1, reloadedStore.failureCount())
    }

    @Test
    fun cooldownRemainsActiveInMemoryWhenMarkerCannotBeWritten() {
        val invalidConfigDir = File(tempDir.root, "not-a-directory").apply { createNewFile() }
        val storeWithUnwritableMarker = RemoteConfigStore(invalidConfigDir, versionCode)
        val cooldownUntil = System.currentTimeMillis() + 60_000

        storeWithUnwritableMarker.setCooldownUntil(cooldownUntil)

        assertEquals(cooldownUntil, storeWithUnwritableMarker.cooldownUntil())
    }

    @Test
    fun cooldownMarkerBeyondMaximumIsDeleted() {
        val markerFile = File(tempDir.root, "core-$versionCode.json.cooldown")
        markerFile.writeText("9999999999999\n3")

        val reloadedStore = RemoteConfigStore(tempDir.root, versionCode)

        assertEquals(0L, reloadedStore.cooldownUntil())
        assertEquals(0, reloadedStore.failureCount())
        assertFalse(markerFile.exists())
    }

    @Test
    fun legacyCooldownMarkerWithMaximumJitterIsKept() {
        // pre-backoff SDKs wrote cooldowns of up to 24h + 2h jitter
        val cooldownUntil = System.currentTimeMillis() + TimeUnit.HOURS.toMillis(26) - 1_000
        File(tempDir.root, "core-$versionCode.json.cooldown").writeText(cooldownUntil.toString())

        val reloadedStore = RemoteConfigStore(tempDir.root, versionCode)

        assertEquals(cooldownUntil, reloadedStore.cooldownUntil())
    }

    @Test
    fun cooldownMarkerIsDeletedWhenClockMovesBackwards() {
        var now = TimeUnit.DAYS.toMillis(1000)
        val clock = RemoteConfigClock { now }
        val writer = RemoteConfigStore(tempDir.root, versionCode, clock = clock)
        writer.setCooldownUntil(now + TimeUnit.HOURS.toMillis(24), 2)

        // the device clock jumps back a week, so the marker now looks ~8 days away
        now -= TimeUnit.DAYS.toMillis(7)
        val reloadedStore = RemoteConfigStore(tempDir.root, versionCode, clock = clock)

        assertEquals(0L, reloadedStore.reloadCooldownUntil())
        assertFalse(File(tempDir.root, "core-$versionCode.json.cooldown").exists())
    }

    @Test
    fun unreadableCooldownMarkerIsIgnored() {
        File(tempDir.root, "core-$versionCode.json.cooldown").mkdirs()

        assertEquals(0L, store.cooldownUntil())
    }

    @Test
    fun crossProcessLockReloadsConfigAndCooldownPersistedByAnotherStore() {
        val config = createValidRemoteConfig("tag1", futureDate(60_000))
        val cooldownUntil = System.currentTimeMillis() + 60_000
        store.store(config)
        store.setCooldownUntil(cooldownUntil)

        val secondStore = RemoteConfigStore(tempDir.root, versionCode)
        val result = secondStore.withCrossProcessLock {
            Pair(secondStore.reloadCurrentOrExpired(), secondStore.reloadCooldownUntil())
        }

        assertEquals("tag1", result?.first?.configurationTag)
        assertEquals(cooldownUntil, result?.second)
    }

    @Test
    fun crossProcessLockWaitsForAnotherStoreToReleaseIt() {
        val secondStore = RemoteConfigStore(tempDir.root, versionCode)
        val lockAcquired = CountDownLatch(1)
        val releaseLock = CountDownLatch(1)
        val executor = Executors.newFixedThreadPool(2)

        try {
            val firstResult = executor.submit<String?> {
                store.withCrossProcessLock {
                    lockAcquired.countDown()
                    releaseLock.await(5, TimeUnit.SECONDS)
                    "first"
                }
            }
            assertTrue(lockAcquired.await(5, TimeUnit.SECONDS))

            val secondResult = executor.submit<String?> {
                secondStore.withCrossProcessLock { "second" }
            }
            assertFalse(secondResult.isDone)

            releaseLock.countDown()
            assertEquals("first", firstResult.get(5, TimeUnit.SECONDS))
            assertEquals("second", secondResult.get(5, TimeUnit.SECONDS))
        } finally {
            releaseLock.countDown()
            executor.shutdownNow()
        }
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
    fun loadPreservesExpiredConfigOnDisk() {
        val expiredConfig = createValidRemoteConfig("expired", pastDate(1000))
        store.store(expiredConfig)

        // Load should return null for expired config
        assertNull(store.load())

        // Config file should be preserved on disk for revalidation / fallback
        val configFile = File(tempDir.root, "core-$versionCode.json")
        assertTrue(configFile.exists())
        assertEquals("expired", store.currentOrExpired()?.configurationTag)
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
        val currentCooldownFile = File(tempDir.root, "core-$versionCode.json.cooldown")

        tempDir.root.mkdirs()
        oldFile1.createNewFile()
        oldFile2.createNewFile()
        currentFile.createNewFile()
        currentCooldownFile.createNewFile()

        store.sweep()

        assertFalse(oldFile1.exists())
        assertFalse(oldFile2.exists())
        assertTrue(currentFile.exists())
        assertTrue(currentCooldownFile.exists())
    }

    @Test
    fun sweepRemovesOldVersionCooldownAndLockButKeepsCurrentTempFile() {
        tempDir.root.mkdirs()
        val oldCooldown = File(tempDir.root, "core-100.json.cooldown").apply { writeText("9999999999999\n4") }
        val oldLock = File(tempDir.root, "core-100.json.lock").apply { createNewFile() }
        val currentTemp = File(tempDir.root, "core-$versionCode.json.new").apply { createNewFile() }
        val currentLock = File(tempDir.root, "core-$versionCode.json.lock").apply { createNewFile() }

        store.sweep()

        assertFalse(oldCooldown.exists())
        assertFalse(oldLock.exists())
        assertTrue(currentTemp.exists())
        assertTrue(currentLock.exists())
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

        // But file should remain on disk so currentOrExpired can revalidate
        val configFile = File(tempDir.root, "core-$versionCode.json")
        assertTrue(configFile.exists())
        assertEquals("expired", newStore.currentOrExpired()?.configurationTag)
    }

    @Test
    fun configFileNameIncludesVersionCode() {
        val config = createValidRemoteConfig("test", futureDate(1000))
        store.store(config)

        val expectedFile = File(tempDir.root, "core-$versionCode.json")
        assertTrue(expectedFile.exists())
    }

    private fun createValidRemoteConfig(tag: String, expiry: Date): RemoteConfig {
        return RemoteConfig(
            configurationTag = tag,
            configurationExpiry = expiry,
            discardRules = emptyList()
        )
    }

    private fun futureDate(offsetMs: Long): Date {
        return Date(System.currentTimeMillis() + offsetMs)
    }

    private fun pastDate(offsetMs: Long): Date {
        return Date(System.currentTimeMillis() - offsetMs)
    }
}
