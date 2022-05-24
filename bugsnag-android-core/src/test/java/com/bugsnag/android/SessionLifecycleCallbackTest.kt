package com.bugsnag.android

import android.app.Activity
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
internal class SessionLifecycleCallbackTest {

    private lateinit var callbacks: SessionLifecycleCallback

    @Mock
    lateinit var tracker: SessionTracker

    @Mock
    lateinit var activity: Activity

    @Before
    fun setUp() {
        callbacks = SessionLifecycleCallback(tracker)
    }

    @Test
    fun onActivityStarted() {
        callbacks.onActivityStarted(activity)
        verify(tracker, times(1)).onActivityStarted("Activity")
    }

    @Test
    fun onActivityPostStarted() {
        callbacks.onActivityPostStarted(activity)
        verify(tracker, times(1)).onActivityStarted("Activity")
    }

    @Test
    fun onActivityStopped() {
        callbacks.onActivityStopped(activity)
        verify(tracker, times(1)).onActivityStopped("Activity")
    }

    @Test
    fun onActivityPostStopped() {
        callbacks.onActivityPostStopped(activity)
        verify(tracker, times(1)).onActivityStopped("Activity")
    }
}
