package com.bugsnag.android

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.concurrent.CountDownLatch

class BeforeSendSessionTest {

    private val client = BugsnagTestUtils.generateClient()
    private val sessionTracker = client.sessionTracker

    @Before
    fun setUp() {
    }

    @Test
    fun testBeforeSendCallbackInvoked() {
        val latch = CountDownLatch(1)
        var data: SessionTrackingPayload? = null

        client.config.addBeforeSendSession { it.device["foo"] = "bar" }
        client.config.addBeforeSendSession { it.device["foo2"] = 5 }

        client.config.delivery = object : Delivery {
            override fun deliver(payload: SessionTrackingPayload, config: Configuration) {
                data = payload
                latch.countDown()
            }

            override fun deliver(report: Report, config: Configuration) {}
        }

        sessionTracker.startSession(false)
        latch.await()

        assertEquals("bar", data!!.device["foo"])
        assertEquals(5, data!!.device["foo2"])
    }
}
