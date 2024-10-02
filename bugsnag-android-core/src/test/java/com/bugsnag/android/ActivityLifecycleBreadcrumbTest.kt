package com.bugsnag.android

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
internal class ActivityLifecycleBreadcrumbTest {

    private lateinit var tracker: ActivityBreadcrumbCollector

    @Mock
    lateinit var activity: Activity

    @Mock
    lateinit var activity2: Activity

    @Mock
    lateinit var bundle: Bundle

    var resultActivity: String? = null
    var resultMetadata: Map<String, Any>? = null

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

    @Test
    fun interleavedStateChanges() {
        tracker.onActivityCreated(activity, null)
        assertNull(resultMetadata!!["previous"])
        tracker.onActivityCreated(activity2, null)
        assertNull(resultMetadata!!["previous"])

        tracker.onActivityStarted(activity)
        assertEquals("onCreate()", resultMetadata!!["previous"])
        tracker.onActivityResumed(activity)
        assertEquals("onStart()", resultMetadata!!["previous"])

        tracker.onActivityStarted(activity2)
        assertEquals("onCreate()", resultMetadata!!["previous"])
    }

    @Test
    fun failGetExtras() {
        val mockIntent = mock(Intent::class.java)
        `when`(mockIntent.extras).thenThrow(NullPointerException())
        `when`(activity.intent).thenReturn(mockIntent)

        tracker.onActivityCreated(activity, null)
        assertFalse(resultMetadata!!["hasBundle"] as Boolean)
        assertFalse(resultMetadata!!["hasData"] as Boolean)
    }
}
