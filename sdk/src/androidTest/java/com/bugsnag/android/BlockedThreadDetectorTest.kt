package com.bugsnag.android

import android.os.Looper
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class BlockedThreadDetectorTest {

    @Test(expected = IllegalArgumentException::class)
    fun testInvalidBlockedThresholdMs() {
        BlockedThreadDetector(-1, 1, Looper.myLooper()) {}
    }

    @Test(expected = IllegalArgumentException::class)
    fun testInvalidCheckIntervalMs() {
        BlockedThreadDetector(1, -1, Looper.myLooper()) {}
    }

    @Test(expected = IllegalArgumentException::class)
    fun testInvalidThread() {
        BlockedThreadDetector(1, 1, null) {}
    }

    @Test(expected = IllegalArgumentException::class)
    fun testInvalidDelegate() {
        BlockedThreadDetector(1, 1, Looper.myLooper(), null)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testExcessiveCheckInterval() {
        BlockedThreadDetector(100, 1000, Looper.myLooper()) {}
    }
}
