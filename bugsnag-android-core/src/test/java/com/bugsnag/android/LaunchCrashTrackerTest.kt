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
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit

@RunWith(MockitoJUnitRunner::class)
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
        val executor = mock(ScheduledThreadPoolExecutor::class.java)
        LaunchCrashTracker(
            convert(
                generateConfiguration().apply {
                    launchDurationMillis = 1
                }
            ),
            executor
        )
        verify(executor, times(1)).schedule(
            any(Runnable::class.java),
            eq(1L),
            eq(TimeUnit.MILLISECONDS)
        )
    }
}
