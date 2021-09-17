package com.bugsnag.android

import android.content.Context
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.junit.MockitoJUnitRunner

/**
 * Test the contract for `AppWithState.inForeground` and `AppWithState.durationInForeground` as
 * produced by `AppDataCollector.generateAppWithState`
 */
@RunWith(MockitoJUnitRunner::class)
class AppDataCollectorForegroundTest {

    @Mock
    internal lateinit var appContext: Context

    @Mock
    internal lateinit var sessionTracker: SessionTracker

    @Mock
    internal lateinit var launchCrashTracker: LaunchCrashTracker

    @Mock
    internal lateinit var memoryTrimState: MemoryTrimState

    private lateinit var appDataCollector: AppDataCollector

    @Before
    fun setUp() {
        `when`(appContext.packageName).thenReturn("test.package.name")

        appDataCollector = AppDataCollector(
            appContext,
            null,
            BugsnagTestUtils.generateImmutableConfig(),
            sessionTracker,
            null,
            launchCrashTracker,
            memoryTrimState
        )
    }

    /**
     * When the app is detected "in the foreground" both properties should be populated as such
     */
    @Test
    fun durationInForeground() {
        val time = System.currentTimeMillis()
        val lastForegroundTime = time - 1000L

        `when`(sessionTracker.isInForeground).thenReturn(true)
        `when`(sessionTracker.lastEnteredForegroundMs).thenReturn(lastForegroundTime)

        val appWithState = appDataCollector.generateAppWithState()
        assertEquals(true, appWithState.inForeground)
        // allow for 20ms of error
        assertTrue(
            "Unexpected durationInForeground: ${appWithState.durationInForeground}",
            appWithState.durationInForeground in 1000L..1020L
        )

        verify(sessionTracker, times(1)).isInForeground
    }

    /**
     * When the app is detected "in the background", the durationInForeground should be zero (0)
     */
    @Test
    fun inBackground() {
        `when`(sessionTracker.isInForeground).thenReturn(false)
        val appWithState = appDataCollector.generateAppWithState()
        assertEquals(false, appWithState.inForeground)
        assertEquals(0L, appWithState.durationInForeground)
    }

    /**
     * When the foreground detection is not possible, both properties should be `null`
     */
    @Test
    fun foregroundNotAvailable() {
        `when`(sessionTracker.isInForeground).thenReturn(null)
        val appWithState = appDataCollector.generateAppWithState()
        assertNull(appWithState.inForeground)
        assertNull(appWithState.durationInForeground)
    }
}
