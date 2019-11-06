package com.bugsnag.android

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
internal class SessionTrackerBreadcrumbTest {

    private lateinit var tracker: SessionTracker

    @Mock
    lateinit var client: Client

    @Mock
    internal var appData: AppData? = null

    @Mock
    internal var deviceData: DeviceData? = null

    @Mock
    lateinit var context: Context

    @Mock
    lateinit var activity: Activity

    @Mock
    lateinit var activityManager: ActivityManager

    @Mock
    lateinit var sessionStore: SessionStore

    @Before
    fun setUp() {
        `when`(client.getAppContext()).thenReturn(context)
        `when`(client.getAppData()).thenReturn(appData)
        `when`(client.getDeviceData()).thenReturn(deviceData)
        `when`(context.getSystemService("activity")).thenReturn(activityManager)
        tracker = SessionTracker(
            BugsnagTestUtils.generateImmutableConfig(),
            CallbackState(),
            client,
            sessionStore,
            NoopLogger
        )
    }

    @Test
    fun onCreateBreadcrumb() {
        tracker.onActivityCreated(activity, null)
        verifyLifecycleBreadcrumb("onCreate()")
    }

    @Test
    fun onStartBreadcrumb() {
        tracker.onActivityStarted(activity)
        verifyLifecycleBreadcrumb("onStart()")
    }

    @Test
    fun onResumeBreadcrumb() {
        tracker.onActivityResumed(activity)
        verifyLifecycleBreadcrumb("onResume()")
    }

    @Test
    fun onPauseBreadcrumb() {
        tracker.onActivityPaused(activity)
        verifyLifecycleBreadcrumb("onPause()")
    }

    @Test
    fun onStopBreadcrumb() {
        tracker.onActivityStopped(activity)
        verifyLifecycleBreadcrumb("onStop()")
    }

    @Test
    fun onSaveInstanceState() {
        tracker.onActivitySaveInstanceState(activity, null)
        verifyLifecycleBreadcrumb("onSaveInstanceState()")
    }

    @Test
    fun onDestroyBreadcrumb() {
        tracker.onActivityDestroyed(activity)
        verifyLifecycleBreadcrumb("onDestroy()")
    }

    private fun verifyLifecycleBreadcrumb(method: String) {
        verify(client, times(1)).leaveBreadcrumb(
            "Activity",
            BreadcrumbType.NAVIGATION,
            mapOf(Pair("ActivityLifecycle", method))
        )
    }
}
