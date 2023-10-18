package com.bugsnag.android.internal

import android.app.Activity
import android.os.Message
import com.bugsnag.android.internal.ForegroundDetector.MSG_SEND_BACKGROUND
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class ForegroundDetectorTest {

    @Mock
    lateinit var testActivity: Activity

    @Test
    fun backgroundToForeground() {
        ForegroundDetector.onActivityCreated(testActivity, null)
        assertFalse(ForegroundDetector.isInForeground)
        ForegroundDetector.onActivityStarted(testActivity)
        assertTrue(ForegroundDetector.isInForeground)
    }

    @Test
    fun backgroundDelayedMessage() {
        var callbackTriggered = false

        val foregroundCallback = object : ForegroundDetector.OnActivityCallback {
            override fun onForegroundStatus(foreground: Boolean, timestamp: Long) {
                callbackTriggered = true
            }

            override fun onActivityStarted(activity: Activity) = Unit
            override fun onActivityStopped(activity: Activity) = Unit
        }

        ForegroundDetector.registerActivityCallbacks(foregroundCallback)
        ForegroundDetector.backgroundSent = false
        ForegroundDetector.isInForeground = true

        val message = Message()
        message.what = MSG_SEND_BACKGROUND
        message.arg1 = -1 // 0xffffffff
        message.arg2 = -1 // 0xffffffff

        assertTrue(ForegroundDetector.handleMessage(message))
        assertEquals(-1 /* 0xffffffffffffffff */, ForegroundDetector.lastExitedForegroundMs)
        assertFalse(ForegroundDetector.isInForeground)
        assertTrue(ForegroundDetector.backgroundSent)
        assertTrue(callbackTriggered)
    }
}
