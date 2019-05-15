package com.bugsnag.android

import android.support.test.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class BeforeSendSessionTest {

    @Test
    fun testBeforeSendCallbackInvoked() {
        val latch = CountDownLatch(1)
        var data: SessionTrackingPayload? = null

        val config = Configuration("api-key")
        config.addBeforeSendSession { it.device["foo"] = "bar" }
        config.addBeforeSendSession { it.device["foo2"] = 5 }

        config.delivery = object : Delivery {
            override fun deliver(payload: SessionTrackingPayload, config: Configuration) {
                data = payload
                latch.countDown()
            }

            override fun deliver(report: Report, config: Configuration) {}
        }

        val client = Client(InstrumentationRegistry.getContext(), config)
        client.sessionTracker.startSession(false)
        latch.await(100, TimeUnit.MILLISECONDS)

        assertEquals("bar", data!!.device["foo"])
        assertEquals(5, data!!.device["foo2"])
    }
}
