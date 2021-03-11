package com.bugsnag.android

import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Date
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

private const val SESSION_CONFINEMENT_ATTEMPTS = 20

/**
 * Confirms that delivery of sessions is confined to a single thread, resulting in no
 * duplicate requests.
 */
internal class SessionTrackerConfinementTest {

    @Test
    fun trackingSessionsIsThreadConfined() {
        // setup delivery for interception
        val retainingDelivery = RetainingDelivery()
        val config = BugsnagTestUtils.generateConfiguration().apply {
            autoTrackSessions = false
            delivery = retainingDelivery
        }
        val client = Client(ApplicationProvider.getApplicationContext(), config)

        // send 20 sessions
        repeat(SESSION_CONFINEMENT_ATTEMPTS) { count ->
            client.sessionTracker.startNewSession(Date(0), User("$count"), false)
        }
        retainingDelivery.latch.await(1, TimeUnit.SECONDS)

        // confirm that no dupe requests are sent and that the request order is deterministic
        assertEquals(SESSION_CONFINEMENT_ATTEMPTS, retainingDelivery.payloads.size)

        retainingDelivery.payloads.forEachIndexed { index, session ->
            assertEquals("$index", session.getUser().id)
        }
    }

    /**
     * Retains all the sent session payloads
     */
    private class RetainingDelivery : Delivery {
        val payloads = mutableListOf<Session>()
        val latch = CountDownLatch(SESSION_CONFINEMENT_ATTEMPTS)

        override fun deliver(payload: Session, deliveryParams: DeliveryParams): DeliveryStatus {
            payloads.add(payload)
            latch.countDown()
            return DeliveryStatus.DELIVERED
        }

        override fun deliver(payload: EventPayload, deliveryParams: DeliveryParams) =
            DeliveryStatus.DELIVERED
    }
}
