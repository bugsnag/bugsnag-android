package com.bugsnag.android

import com.bugsnag.android.BugsnagTestUtils.convert
import com.bugsnag.android.BugsnagTestUtils.generateConfiguration
import com.bugsnag.android.BugsnagTestUtils.generateImmutableConfig
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.junit.MockitoJUnitRunner
import java.io.File
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit

@RunWith(MockitoJUnitRunner::class)
class LaunchCrashTrackerTest {

    private val cfg = generateImmutableConfig()

    @Test
    fun defaultLaunchPeriodManualMark() {
        val tracker = LaunchCrashTracker(cfg)
        assertFalse(tracker.isLaunching())

        tracker.startAutoTracking(cfg)
        assertTrue(tracker.isLaunching())

        tracker.markLaunchCompleted()
        assertFalse(tracker.isLaunching())
    }

    @Test
    fun zeroLaunchPeriodManualMark() {
        val config = convert(generateConfiguration().apply { launchDurationMillis = 0 })
        val tracker = LaunchCrashTracker(config)
        assertFalse(tracker.isLaunching())

        tracker.startAutoTracking(config)
        assertTrue(tracker.isLaunching())

        java.lang.Thread.sleep(10)
        assertTrue(tracker.isLaunching())
        tracker.markLaunchCompleted()
        assertFalse(tracker.isLaunching())
    }

    @Test
    fun smallLaunchPeriodAutomatic() {
        val executor = mock(ScheduledThreadPoolExecutor::class.java)
        val config = convert(generateConfiguration().apply { launchDurationMillis = 1 })
        val tracker = LaunchCrashTracker(config, executor)
        tracker.startAutoTracking(config)
        verify(executor, times(1)).schedule(
            any(Runnable::class.java),
            eq(1L),
            eq(TimeUnit.MILLISECONDS)
        )
    }

    @Test
    fun crashedLastLaunchMarkStarted() {
        with(LaunchCrashTracker(cfg)) {
            markLaunchStarted()
            assertFalse(crashedDuringLastLaunch())
        }
    }

    @Test
    fun crashedLastLaunchMarkCompleted() {
        with(LaunchCrashTracker(cfg)) {
            markLaunchStarted()
            markLaunchCompleted()
            assertFalse(crashedDuringLastLaunch())
        }
    }

    @Test
    fun testCrashedDuringLastLaunch() {
        val marker = File(cfg.persistenceDirectory.value, "launch-crash-marker")
        marker.createNewFile()

        val tracker = LaunchCrashTracker(cfg)
        assertTrue(tracker.crashedDuringLastLaunch())

        marker.delete()
        assertFalse(tracker.crashedDuringLastLaunch())
    }

    @Test
    fun trackerCreatesMarkerFile() {
        val marker = File(cfg.persistenceDirectory.value, "launch-crash-marker")
        assertFalse(marker.exists())

        val tracker = LaunchCrashTracker(cfg)
        tracker.markLaunchStarted()
        assertTrue(marker.exists())

        tracker.markLaunchCompleted()
        assertFalse(marker.exists())
    }
}
