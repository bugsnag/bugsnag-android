package com.bugsnag.android.internal

import android.app.Activity
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
}
