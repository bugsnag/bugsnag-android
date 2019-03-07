package com.bugsnag.android

import android.os.Looper
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

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

    @Test(expected = IllegalArgumentException::class)
    fun testExcessiveCheckInterval() {
        BlockedThreadDetector(100, 1000, looper) {}
    }

    /**
     * Verifies that the thread is not detected as blocked if it completes within a reasonable time
     */
    @Test
    fun testLiveThreadNotDetected() {
        val latch = CountDownLatch(1)
        blockThread(latch, 100)
        assertEquals(1, latch.count)
    }

    /**
     * Verifies that the thread is detected as blocked if it does not complete within its
     * configured threshold.
     */
    @Test
    fun testBlockedThreadDetected() {
        val latch = CountDownLatch(1)
        blockThread(latch, 1)
        assertEquals(0, latch.count)
    }

    /**
     * Blocks the main thread with [CountDownLatch.await], meaning the delegate should be invoked
     * once after the configured threshold has been exceeded. This will decrement
     * [CountDownLatch.getCount], which can then be used to verify behaviour of the delegate.
     */
    private fun blockThread(latch: CountDownLatch, thresholdMs: Long): Boolean {
        BlockedThreadDetector(thresholdMs, 1, looper) {
            latch.countDown()
        }.start()
        return latch.await(15, TimeUnit.MILLISECONDS)
    }
}
