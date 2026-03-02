package com.bugsnag.android

import android.os.Handler
import android.os.HandlerThread
import com.bugsnag.android.internal.LooperMonitorThread
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.lang.Thread as JThread

private const val APP_HANG_THRESHOLD = 200L

/**
 * Tests for LooperMonitorThread without cooldown period configured.
 * All detected AppHangs should be reported.
 */
class SequentialAppHangsTest {
    private lateinit var handlerThread: HandlerThread
    private lateinit var monitorThread: LooperMonitorThread
    private lateinit var handler: Handler

    private var appHangCount = 0

    @Before
    fun setup() {
        appHangCount = 0

        handlerThread = HandlerThread("Test Thread")
        handlerThread.start()
        handler = Handler(handlerThread.looper)

        monitorThread = LooperMonitorThread(
            watchedLooper = handlerThread.looper,
            appHangThresholdMillis = APP_HANG_THRESHOLD,
            appHangCooldownMillis = 0L, // No cooldown
            samplingThresholdMillis = 0,
            samplingRateMillis = 0,
            onAppHangDetected = { _, _ -> appHangCount++ }
        )

        monitorThread.startMonitoring()
    }

    @After
    fun shutdown() {
        monitorThread.stopMonitoring()
        handlerThread.quit()
    }

    @Test
    fun testIdleHandlerThread() {
        JThread.sleep(APP_HANG_THRESHOLD * 5)
        assertEquals("no AppHangs expected", 0, appHangCount)
    }

    @Test
    fun testBelowThresholdEvents() {
        val countDownLatch = CountDownLatch(10)
        val task = object : Runnable {
            override fun run() {
                JThread.sleep((APP_HANG_THRESHOLD / 2) - 10)
                countDownLatch.countDown()

                if (countDownLatch.count > 0) {
                    handler.postDelayed(this, 10L)
                }
            }
        }
        handler.postDelayed(task, 1)

        countDownLatch.await()
        assertEquals("no AppHangs expected", 0, appHangCount)
    }

    @Test
    fun appHang() {
        val countDownLatch = CountDownLatch(1)
        handler.postDelayed({
            // wait long enough for 2+ AppHang triggers to happen
            JThread.sleep(APP_HANG_THRESHOLD * 3)
            countDownLatch.countDown()
        }, 1)

        countDownLatch.await()

        assertEquals("exactly 1 AppHang expected", 1, appHangCount)
    }

    @Test
    fun appHangRecoverHang() {
        val countDownLatch = CountDownLatch(2)

        handler.postDelayed({
            JThread.sleep(APP_HANG_THRESHOLD * 2)
            countDownLatch.countDown()

            handler.postDelayed({
                // Without cooldown, this AppHang should also be reported
                JThread.sleep(APP_HANG_THRESHOLD * 2)
                countDownLatch.countDown()
            }, 100L) // Small delay to ensure recovery between hangs
        }, 1)

        countDownLatch.await()

        // Without cooldown, both AppHangs should be reported
        assertEquals("exactly 2 AppHangs expected", 2, appHangCount)
    }

    @Test
    fun multipleSequentialHangs() {
        val countDownLatch = CountDownLatch(3)

        handler.postDelayed({
            JThread.sleep(APP_HANG_THRESHOLD * 2)
            countDownLatch.countDown()

            handler.postDelayed({
                JThread.sleep(APP_HANG_THRESHOLD * 2)
                countDownLatch.countDown()

                handler.postDelayed({
                    JThread.sleep(APP_HANG_THRESHOLD * 2)
                    countDownLatch.countDown()
                }, 100L)
            }, 100L)
        }, 1)

        countDownLatch.await()

        // Without cooldown, all 3 AppHangs should be reported
        assertEquals("exactly 3 AppHangs expected", 3, appHangCount)
    }
}
