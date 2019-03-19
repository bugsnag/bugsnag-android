package com.bugsnag.android

import android.os.Looper
import org.junit.Test

class BlockedThreadDetectorTest {

    private val looper = Looper.getMainLooper()

    @Test(expected = IllegalArgumentException::class)
    fun testInvalidBlockedThresholdMs() {
        BlockedThreadDetector(-1, 1, looper) {}
    }

    @Test(expected = IllegalArgumentException::class)
    fun testInvalidCheckIntervalMs() {
        BlockedThreadDetector(1, -1, looper) {}
    }

    @Test(expected = IllegalArgumentException::class)
    fun testInvalidThread() {
        BlockedThreadDetector(1, 1, null) {}
    }

    @Test(expected = IllegalArgumentException::class)
    fun testInvalidDelegate() {
        BlockedThreadDetector(1, 1, looper, null)
    }
}
