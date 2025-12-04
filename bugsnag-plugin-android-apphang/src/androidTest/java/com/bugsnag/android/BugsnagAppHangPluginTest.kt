package com.bugsnag.android

import android.os.Handler
import android.os.HandlerThread
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import java.util.concurrent.CountDownLatch
import java.lang.Thread as JThread

private const val APP_HANG_THRESHOLD = 100L

class BugsnagAppHangPluginTest {
    private lateinit var handlerThread: HandlerThread
    private lateinit var plugin: BugsnagAppHangPlugin
    private lateinit var client: Client
    private lateinit var handler: Handler

    @Before
    fun setup() {
        handlerThread = HandlerThread("Test Thread")
        handlerThread.start()
        handler = Handler(handlerThread.looper)

        plugin = BugsnagAppHangPlugin(
            AppHangConfiguration(
                appHangThresholdMillis = APP_HANG_THRESHOLD,
                watchedLooper = handlerThread.looper
            )
        )

        client = Mockito.mock()
        plugin.load(client)
        plugin.startMonitoring()
    }

    @After
    fun shutdown() {
        plugin.unload()
        handlerThread.quit()
    }

    @Test
    fun testIdleHandlerThread() {
        JThread.sleep(APP_HANG_THRESHOLD * 5)
        verifyNoInteractions(client)
    }

    @Test
    fun testBelowThresholdEvents() {
        val countDownLatch = CountDownLatch(10)
        repeat(countDownLatch.count.toInt()) {
            handler.post {
                JThread.sleep(APP_HANG_THRESHOLD / 2)
                countDownLatch.countDown()
            }
        }

        verifyNoInteractions(client)
    }

    @Test
    fun appHang() {
        val countDownLatch = CountDownLatch(1)
        handler.post {
            // wait long enough for 2+ AppHang triggers to happen
            JThread.sleep(APP_HANG_THRESHOLD * 3)
            countDownLatch.countDown()
        }

        countDownLatch.await()

        // we should have reported exactly 1 AppHang
        verify(client, times(1))
            .notify(any(AppHangException::class.java), any())
    }

    @Test
    fun appHangRecoverHang() {
        val countDownLatch = CountDownLatch(2)

        handler.post {
            // wait long enough for 2+ AppHang triggers to happen
            JThread.sleep(APP_HANG_THRESHOLD * 3)
            countDownLatch.countDown()

            handler.postDelayed({
                JThread.sleep(APP_HANG_THRESHOLD * 3)
                countDownLatch.countDown()
            }, APP_HANG_THRESHOLD)
        }

        countDownLatch.await()

        // we should have reported exactly 1 AppHangs
        verify(client, times(2))
            .notify(any(AppHangException::class.java), any())
    }
}
