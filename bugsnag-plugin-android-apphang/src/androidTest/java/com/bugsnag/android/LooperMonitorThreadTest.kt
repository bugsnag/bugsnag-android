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

private const val APP_HANG_THRESHOLD = 100L
private const val RECOVERY_TIME = 800L

class LooperMonitorThreadTest {
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
            recoveryTimeMillis = RECOVERY_TIME,
            onAppHangDetected = { appHangCount++ }
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
                JThread.sleep(APP_HANG_THRESHOLD / 2)
                countDownLatch.countDown()

                if (countDownLatch.count > 0) {
                    handler.postDelayed(this, 1L)
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
        val countDownLatch = CountDownLatch(3)

        handler.postDelayed({
            JThread.sleep(APP_HANG_THRESHOLD * 2)
            countDownLatch.countDown()

            handler.postDelayed({
                // this AppHang is within the recovery time, so should be ignored
                JThread.sleep(APP_HANG_THRESHOLD * 2)
                countDownLatch.countDown()

                handler.postDelayed({
                    // outside the recovery time now, so this should be reported
                    JThread.sleep(APP_HANG_THRESHOLD * 2)
                    countDownLatch.countDown()
                }, RECOVERY_TIME * 2)
            }, RECOVERY_TIME / 2)
        }, 1)

        countDownLatch.await()

        // we should have reported exactly 2 AppHangs
        assertEquals("exactly 2 AppHangs expected", 2, appHangCount)
    }
}
