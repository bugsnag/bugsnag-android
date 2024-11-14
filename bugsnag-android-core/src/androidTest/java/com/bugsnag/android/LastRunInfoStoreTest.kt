package com.bugsnag.android

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.bugsnag.android.BugsnagTestUtils.generateConfiguration
import com.bugsnag.android.internal.convertToImmutableConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

internal class LastRunInfoStoreTest {

    lateinit var file: File
    lateinit var lastRunInfoStore: LastRunInfoStore

    @Before
    fun setUp() {
        val config = convertToImmutableConfig(
            generateConfiguration().apply {
                persistenceDirectory = ApplicationProvider.getApplicationContext<Context>().cacheDir
            },
            packageInfo = null,
            appInfo = null
        )
        file = File(config.persistenceDirectory.value, "bugsnag/last-run-info")
        file.delete()
        lastRunInfoStore = LastRunInfoStore(config)
    }

    /**
     * Null should be returned for non-existent files
     */
    @Test
    fun readNonExistentFile() {
        assertNull(lastRunInfoStore.load())
    }

    /**
     * Null should be returned for empty files
     */
    @Test
    fun readEmptyFile() {
        file.createNewFile()
        assertNull(lastRunInfoStore.load())
    }

    /**
     * Null should be returned for invalid file contents
     */
    @Test
    fun readInvalidFileContents() {
        file.writeText("{\"hamster\": 2}")
        assertNull(lastRunInfoStore.load())
    }

    /**
     * Null should be returned for invalid file contents
     */
    @Test
    fun readInvalidFileContents2() {
        file.writeText("consecutiveLaunchCrashes=ban_ana\ncrashed=509f%\ncrashed=apple\n")
        assertNull(lastRunInfoStore.load())
    }

    /**
     * Information should be returned for valid files
     */
    @Test
    fun readValidFileContents() {
        file.parentFile?.mkdirs()
        file.writeText("consecutiveLaunchCrashes=5\ncrashed=true\ncrashedDuringLaunch=false\n")
        val info = requireNotNull(lastRunInfoStore.load())
        assertEquals(5, info.consecutiveLaunchCrashes)
        assertTrue(info.crashed)
        assertFalse(info.crashedDuringLaunch)
    }

    @Test
    fun writableFileNoCrash() {
        lastRunInfoStore.persist(
            LastRunInfo(
                consecutiveLaunchCrashes = 0,
                crashed = false,
                crashedDuringLaunch = false
            )
        )
        val lines = file.readText().split("\n")
        assertEquals(4, lines.size)
        assertEquals("consecutiveLaunchCrashes=0", lines[0])
        assertEquals("crashed=false", lines[1])
        assertEquals("crashedDuringLaunch=false", lines[2])
        assertEquals("", lines[3])
    }

    @Test
    fun writableFileCrash() {
        lastRunInfoStore.persist(
            LastRunInfo(
                consecutiveLaunchCrashes = 2,
                crashed = true,
                crashedDuringLaunch = true
            )
        )
        val lines = file.readText().split("\n")
        assertEquals(4, lines.size)
        assertEquals("consecutiveLaunchCrashes=2", lines[0])
        assertEquals("crashed=true", lines[1])
        assertEquals("crashedDuringLaunch=true", lines[2])
        assertEquals("", lines[3])
    }

    @Test
    fun nonWritableFile() {
        file.apply {
            delete()
            parentFile?.mkdirs()
            createNewFile()
            setWritable(false)
        }
        assertNull(lastRunInfoStore.load())
    }
}
