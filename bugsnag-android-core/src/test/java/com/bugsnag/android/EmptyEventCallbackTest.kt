package com.bugsnag.android

import com.bugsnag.android.BugsnagTestUtils.generateConfiguration
import com.bugsnag.android.BugsnagTestUtils.generateEvent
import com.bugsnag.android.FileStore.Delegate
import com.bugsnag.android.internal.BackgroundTaskService
import com.bugsnag.android.internal.ImmutableConfig
import com.bugsnag.android.internal.convertToImmutableConfig
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.io.File
import java.nio.file.Files
import java.util.concurrent.CountDownLatch

class EmptyEventCallbackTest {

    private lateinit var storageDir: File
    private lateinit var errorDir: File
    private lateinit var backgroundTaskService: BackgroundTaskService

    @Before
    fun setUp() {
        storageDir = Files.createTempDirectory("tmp").toFile()
        storageDir.deleteRecursively()
        errorDir = File(storageDir, "bugsnag/errors")
        backgroundTaskService = BackgroundTaskService()
    }

    @After
    fun tearDown() {
        storageDir.deleteRecursively()
        backgroundTaskService.shutdown()
    }

    @Test
    fun emptyQueuedEventTriggerEventStoreEmptyCallback() {
        val config = generateConfiguration().apply {
            maxPersistedEvents = 0
            persistenceDirectory = storageDir
        }
        val eventStore = createEventStore(convertToImmutableConfig(config))
        eventStore.write(generateEvent())

        val callbackLatch = CountDownLatch(1)
        eventStore.onEventStoreEmptyCallback = { callbackLatch.countDown() }
        eventStore.flushAsync()
        callbackLatch.await()

        assertTrue(eventStore.isEmpty())
    }

    @Test
    fun testFailedDeliveryEvents() {
        val mockDelivery = mock(Delivery::class.java)
        `when`(mockDelivery.deliver(any<EventPayload>(), any<DeliveryParams>()))
            .thenReturn(
                DeliveryStatus.DELIVERED,
                DeliveryStatus.FAILURE
            )

        val config = generateConfiguration().apply {
            maxPersistedEvents = 3
            persistenceDirectory = storageDir
            delivery = mockDelivery
        }
        val eventStore = createEventStore(convertToImmutableConfig(config))
        repeat(3) {
            eventStore.write(generateEvent())
        }

        // the EventStore should not be considered empty with 3 events in it
        assertFalse(eventStore.isEmpty())

        var eventStoreEmptyCount = 0
        eventStore.onEventStoreEmptyCallback = { eventStoreEmptyCount++ }
        eventStore.flushAsync()
        backgroundTaskService.shutdown()

        assertTrue(
            "there should be no undelivered payloads in the EventStore",
            eventStore.isEmpty()
        )

        assertEquals(
            "onEventStoreEmptyCallback have been called even with a failed (deleted) payload",
            1,
            eventStoreEmptyCount
        )
    }

    @Test
    fun testUndeliveredEvents() {
        val mockDelivery = mock(Delivery::class.java)
        `when`(mockDelivery.deliver(any<EventPayload>(), any<DeliveryParams>()))
            .thenReturn(
                DeliveryStatus.DELIVERED,
                DeliveryStatus.FAILURE,
                DeliveryStatus.UNDELIVERED
            )

        val config = generateConfiguration().apply {
            maxPersistedEvents = 3
            persistenceDirectory = storageDir
            delivery = mockDelivery
        }
        val eventStore = createEventStore(convertToImmutableConfig(config))
        repeat(3) {
            eventStore.write(generateEvent())
        }

        // the EventStore should not be considered empty with 3 events in it
        assertFalse(eventStore.isEmpty())

        var eventStoreEmptyCount = 0
        eventStore.onEventStoreEmptyCallback = { eventStoreEmptyCount++ }
        eventStore.flushAsync()
        backgroundTaskService.shutdown()

        // the last payload should not have been delivered
        assertFalse(
            "there should be one undelivered payload in the EventStore",
            eventStore.isEmpty()
        )

        assertEquals(
            "onEventStoreEmptyCallback should not be called when there are undelivered payloads",
            0,
            eventStoreEmptyCount
        )
    }

    private fun createEventStore(config: ImmutableConfig): EventStore {
        return EventStore(
            config,
            NoopLogger,
            Notifier(),
            backgroundTaskService,
            object : Delegate {
                override fun onErrorIOFailure(
                    exception: Exception?,
                    errorFile: File?,
                    context: String?
                ) {
                }
            },
            CallbackState()
        )
    }
}
