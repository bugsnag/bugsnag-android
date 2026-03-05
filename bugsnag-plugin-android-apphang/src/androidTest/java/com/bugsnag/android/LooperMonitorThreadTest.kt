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

private const val NEAR_HANG_THRESHOLD = 100L
private const val APP_HANG_THRESHOLD = NEAR_HANG_THRESHOLD * 2
private const val COOLDOWN_TIME = 800L

class LooperMonitorThreadTest {
    private lateinit var handlerThread: HandlerThread
    private lateinit var monitorThread: LooperMonitorThread
    private lateinit var handler: Handler

    private var appHangCount = 0
    private var nearHangCount = 0

    @Before
    fun setup() {
        appHangCount = 0

        handlerThread = HandlerThread("Test Thread")
        handlerThread.start()
        handler = Handler(handlerThread.looper)

        monitorThread = LooperMonitorThread(
            watchedLooper = handlerThread.looper,
            appHangThresholdMillis = APP_HANG_THRESHOLD,
            appHangCooldownMillis = COOLDOWN_TIME,
            samplingThresholdMillis = 0,
            samplingRateMillis = 0,
            nearHangThresholdMillis = NEAR_HANG_THRESHOLD,
            onAppHangDetected = { _, _ -> appHangCount++ },
            onNearHangDetected = { _, _ -> nearHangCount++ }
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
        val nearHangTotal = 10
        val countDownLatch = CountDownLatch(nearHangTotal)
        val task = object : Runnable {
            override fun run() {
                JThread.sleep(NEAR_HANG_THRESHOLD)
                countDownLatch.countDown()

                if (countDownLatch.count > 0) {
                    handler.postDelayed(this, 1L)
                }
            }
        }

        handler.postDelayed(task, 1)

        countDownLatch.await()
        // Allow the monitor thread to wake up and detect the final near-hang
        JThread.sleep(APP_HANG_THRESHOLD)
        assertEquals("no AppHangs expected", 0, appHangCount)
        assertEquals("$nearHangTotal NearHangs expected", nearHangTotal, nearHangCount)
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
        val countDownLatch = CountDownLatch(3)

        handler.postDelayed({
            JThread.sleep(APP_HANG_THRESHOLD * 2)
            countDownLatch.countDown()

            handler.postDelayed({
                // This AppHang is within the cooldown period, so should be suppressed
                // Starts 100ms after first ends, detected at 300ms total (well within 800ms cooldown)
                JThread.sleep(APP_HANG_THRESHOLD * 2)
                countDownLatch.countDown()

                handler.postDelayed({
                    // This AppHang is after the cooldown period, so should be reported
                    // Starts 800ms after second ends, giving enough time for cooldown to expire
                    JThread.sleep(APP_HANG_THRESHOLD * 2)
                    countDownLatch.countDown()
                }, COOLDOWN_TIME + 200L)
            }, 100L)
        }, 1)

        countDownLatch.await()

        // First and third AppHangs should be reported, second suppressed by cooldown
        assertEquals("exactly 2 AppHangs expected", 2, appHangCount)
    }
}
