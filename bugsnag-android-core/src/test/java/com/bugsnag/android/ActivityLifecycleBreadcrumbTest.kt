package com.bugsnag.android

import android.app.Activity
import android.os.Bundle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
internal class ActivityLifecycleBreadcrumbTest {

    private lateinit var tracker: ActivityBreadcrumbCollector

    @Mock
    lateinit var activity: Activity

    @Mock
    lateinit var bundle: Bundle

    var resultActivity: String? = null
    var resultMetadata: MutableMap<String, Any?>? = null

    @Before
    fun setUp() {
        resultActivity = null
        resultMetadata = null
        tracker = ActivityBreadcrumbCollector { activity, data ->
            resultActivity = activity
            resultMetadata = data
        }
    }

    @Test
    fun onCreateBreadcrumbNoBundle() {
        tracker.onActivityCreated(activity, null)
        assertEquals("Activity#onCreate()", resultActivity)
        assertFalse(resultMetadata!!["hasBundle"] as Boolean)
    }

    @Test
    fun onCreateBreadcrumbBundle() {
        tracker.onActivityCreated(activity, bundle)
        assertEquals("Activity#onCreate()", resultActivity)
        assertTrue(resultMetadata!!["hasBundle"] as Boolean)
    }

    @Test
    fun onStartBreadcrumb() {
        tracker.onActivityStarted(activity)
        assertEquals("Activity#onStart()", resultActivity)
        assertNull(resultMetadata!!["hasBundle"])
    }

    @Test
    fun onResumeBreadcrumb() {
        tracker.onActivityResumed(activity)
        assertEquals("Activity#onResume()", resultActivity)
        assertNull(resultMetadata!!["hasBundle"])
    }

    @Test
    fun onPauseBreadcrumb() {
        tracker.onActivityPaused(activity)
        assertEquals("Activity#onPause()", resultActivity)
        assertNull(resultMetadata!!["hasBundle"])
    }

    @Test
    fun onStopBreadcrumb() {
        tracker.onActivityStopped(activity)
        assertEquals("Activity#onStop()", resultActivity)
        assertNull(resultMetadata!!["hasBundle"])
    }

    @Test
    fun onSaveInstanceState() {
        tracker.onActivitySaveInstanceState(activity, bundle)
        assertEquals("Activity#onSaveInstanceState()", resultActivity)
        assertNull(resultMetadata!!["hasBundle"])
    }

    @Test
    fun onDestroyBreadcrumb() {
        tracker.onActivityDestroyed(activity)
        assertEquals("Activity#onDestroy()", resultActivity)
        assertNull(resultMetadata!!["hasBundle"])
    }

    @Test
    fun prevStateTest() {
        tracker.onActivityCreated(activity, null)
        assertEquals("Activity#onCreate()", resultActivity)
        assertNull(resultMetadata!!["previous"])

        tracker.onActivityStarted(activity)
        assertEquals("onCreate()", resultMetadata!!["previous"])
    }
}
