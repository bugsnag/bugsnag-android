package com.bugsnag.android

import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

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
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val persistenceDir = File(context.cacheDir, "event-store-confinement")
        persistenceDir.deleteRecursively()
        persistenceDir.mkdirs()

        // setup delivery for interception
        retainingDelivery = RetainingDelivery(EVENT_CONFINEMENT_ATTEMPTS)
        val cfg = BugsnagTestUtils.generateConfiguration(persistenceDir).apply {
            autoTrackSessions = false
            endpoints = EndpointConfiguration(endpoints.notify, endpoints.sessions, null)
            delivery = retainingDelivery
        }
        client = Client(context, cfg)
        waitForErrorTasksToDrain()
    }

    /**
     * Calling delivery of handled events is confined to a single thread
     */
    @Test
    fun notifyIsThreadConfined() {
        // send 20 errors
        repeat(EVENT_CONFINEMENT_ATTEMPTS) { count ->
            val event = Event(
                RuntimeException("$count"),
                client.immutableConfig,
                SeverityReason.newInstance(SeverityReason.REASON_HANDLED_EXCEPTION),
                NoopLogger
            ).apply {
                app = BugsnagTestUtils.generateAppWithState()
                device = BugsnagTestUtils.generateDeviceWithState()
            }
            client.deliveryDelegate.deliver(event)
        }
        waitForErrorTasksToDrain()

        // confirm that no dupe requests are sent and that the request order is deterministic
        val payloads = retainingDelivery.payloadJsons
        val deliveryInvocations = retainingDelivery.deliveryInvocations.get()
        val eventStoreEmpty = client.getEventStore().isEmpty()
        assertEquals(
            "deliveryInvocations=$deliveryInvocations; eventStoreEmpty=$eventStoreEmpty; payloads=$payloads",
            EVENT_CONFINEMENT_ATTEMPTS,
            payloads.size
        )
        assertEquals(
            "deliveryInvocations=$deliveryInvocations; eventStoreEmpty=$eventStoreEmpty; payloads=$payloads",
            EVENT_CONFINEMENT_ATTEMPTS,
            payloads.toSet().size
        )

        payloads.forEachIndexed { index, json ->
            assertTrue(
                "expected payload $index to contain message '$index' but was: $json",
                json.contains("\"message\":\"$index\"")
            )
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
        assertTrue(retainingDelivery.latch.await(10, TimeUnit.SECONDS))
        waitForErrorTasksToDrain()

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
        val payloadJsons = mutableListOf<String>()
        val deliveryInvocations = AtomicInteger(0)
        val latch = CountDownLatch(attempts)

        override fun deliver(payload: Session, deliveryParams: DeliveryParams) =
            DeliveryStatus.DELIVERED

        override fun deliver(
            payload: EventPayload,
            deliveryParams: DeliveryParams
        ): DeliveryStatus {
            deliveryInvocations.incrementAndGet()
            try {
                payloadJsons.add(String(payload.toByteArray(), Charsets.UTF_8))
                payload.eventFile?.let(files::add)
            } catch (exc: Exception) {
                payloadJsons.add("EXCEPTION:${exc::class.java.name}:${exc.message}")
            } finally {
                latch.countDown()
            }
            return DeliveryStatus.DELIVERED
        }
    }

    private fun waitForErrorTasksToDrain() {
        client.deliveryDelegate.backgroundTaskService
            .submitTask(
                com.bugsnag.android.internal.TaskType.ERROR_REQUEST,
                object : Runnable {
                    override fun run() = Unit
                }
            )
            .get(10, TimeUnit.SECONDS)
    }
}
