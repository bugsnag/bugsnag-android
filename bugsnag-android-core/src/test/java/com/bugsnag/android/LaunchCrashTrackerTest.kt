package com.bugsnag.android

import com.bugsnag.android.BugsnagTestUtils.convert
import com.bugsnag.android.BugsnagTestUtils.generateConfiguration
import com.bugsnag.android.BugsnagTestUtils.generateImmutableConfig
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LaunchCrashTrackerTest {

    @Test
    fun defaultLaunchPeriodManualMark() {
        val tracker = LaunchCrashTracker(generateImmutableConfig())
        assertTrue(tracker.isLaunching())
        tracker.markLaunchCompleted()
        assertFalse(tracker.isLaunching())
    }

    @Test
    fun zeroLaunchPeriodManualMark() {
        val tracker = LaunchCrashTracker(
            convert(
                generateConfiguration().apply {
                    launchDurationMillis = 0
                }
            )
        )
        assertTrue(tracker.isLaunching())

        java.lang.Thread.sleep(10)
        assertTrue(tracker.isLaunching())
        tracker.markLaunchCompleted()
        assertFalse(tracker.isLaunching())
    }

    @Test
    fun smallLaunchPeriodAutomatic() {
        val tracker = LaunchCrashTracker(
            convert(
                generateConfiguration().apply {
                    launchDurationMillis = 1
                }
            )
        )
        assertTrue(tracker.isLaunching())

        java.lang.Thread.sleep(10)
        assertFalse(tracker.isLaunching())
    }
}
