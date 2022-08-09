package com.bugsnag.android

import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

private const val EVENT_CONFINEMENT_ATTEMPTS = 20

/**
 * Confirms that delivery of events is confined to a single thread, resulting in no
 * duplicate requests.
 */
internal class EventStoreConfinementTest {

    private lateinit var retainingDelivery: RetainingDelivery
    lateinit var client: Client

    @Before
    fun setUp() {
        // setup delivery for interception
        retainingDelivery = RetainingDelivery(EVENT_CONFINEMENT_ATTEMPTS)
        val cfg = BugsnagTestUtils.generateConfiguration().apply {
            autoTrackSessions = false
            delivery = retainingDelivery
        }
        client = Client(ApplicationProvider.getApplicationContext(), cfg)
    }

    /**
     * Calling notify() is confined to a single thread
     */
    @Test
    fun notifyIsThreadConfined() {
        // send 20 errors
        repeat(EVENT_CONFINEMENT_ATTEMPTS) { count ->
            client.notify(RuntimeException("$count"))
        }
        retainingDelivery.latch.await(10, TimeUnit.SECONDS)

        // confirm that no dupe requests are sent and that the request order is deterministic
        val payloads = retainingDelivery.payloads
        assertEquals(EVENT_CONFINEMENT_ATTEMPTS, payloads.size)
        assertEquals(EVENT_CONFINEMENT_ATTEMPTS, payloads.toSet().size)

        payloads.forEachIndexed { index, event ->
            val exc = requireNotNull(event.originalError)
            assertEquals("$index", exc.message)
        }
    }

    /**
     * Calling flushAsync() is confined to a single thread
     */
    @Test
    fun flushAsyncIsThreadConfined() {
        val eventStore = client.eventStore

        // send 20 errors
        repeat(EVENT_CONFINEMENT_ATTEMPTS) { count ->
            val event = BugsnagTestUtils.generateEvent().apply {
                apiKey = "$count"
            }
            eventStore.write(event)
            eventStore.flushAsync()
        }
        retainingDelivery.latch.await(5, TimeUnit.SECONDS)

        // confirm that no dupe requests are sent
        val filenames = retainingDelivery.files
        assertEquals(EVENT_CONFINEMENT_ATTEMPTS, filenames.size)
        assertEquals(EVENT_CONFINEMENT_ATTEMPTS, filenames.toSet().size)

        val remainingExpectedApiKeys = filenames.indices.mapTo(hashSetOf()) { "$it" }
        retainingDelivery.files.forEachIndexed { index, file ->
            val eventInfo = EventFilenameInfo.fromFile(file, client.immutableConfig)
            assertTrue(
                "unexpected file: $file ($index), expected one of $remainingExpectedApiKeys",
                remainingExpectedApiKeys.remove(eventInfo.apiKey)
            )
        }
    }

    /**
     * Retains all the sent error payloads
     */
    private class RetainingDelivery(attempts: Int) : Delivery {
        val files = mutableListOf<File>()
        val payloads = mutableListOf<Event>()
        val latch = CountDownLatch(attempts)

        override fun deliver(payload: Session, deliveryParams: DeliveryParams) =
            DeliveryStatus.DELIVERED

        override fun deliver(
            payload: EventPayload,
            deliveryParams: DeliveryParams
        ): DeliveryStatus {
            payload.event?.let(payloads::add)
            payload.eventFile?.let(files::add)
            latch.countDown()
            return DeliveryStatus.DELIVERED
        }
    }
}
