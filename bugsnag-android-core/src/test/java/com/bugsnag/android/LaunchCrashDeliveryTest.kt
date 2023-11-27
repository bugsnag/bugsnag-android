package com.bugsnag.android

import com.bugsnag.android.BugsnagTestUtils.generateConfiguration
import com.bugsnag.android.BugsnagTestUtils.generateEvent
import com.bugsnag.android.FileStore.Delegate
import com.bugsnag.android.internal.BackgroundTaskService
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.lang.Thread
import java.nio.file.Files
import java.util.concurrent.atomic.AtomicInteger

/**
 * Verifies that crashes on launch are sent synchronously and other crashes are not.
 */
class LaunchCrashDeliveryTest {

    private lateinit var storageDir: File
    private lateinit var errorDir: File

    @Before
    fun setUp() {
        storageDir = Files.createTempDirectory("tmp").toFile()
        errorDir = File(storageDir, "bugsnag-errors")
    }

    @After
    fun tearDown() {
        storageDir.deleteRecursively()
    }

    /**
     * Verifies that flushOnLaunch() blocks the main thread by simulating a slow delivery.
     */
    @Test
    fun flushOnLaunchBlocksMainThread() {
        val delivery = SlowDelivery()
        val backgroundTaskService = BackgroundTaskService()
        val eventStore = createEventStore(delivery, backgroundTaskService)
        val event = generateEvent()
        event.app.isLaunching = true
        eventStore.write(event)

        // check time difference in ms is >1500, proving thread was blocked
        val baseline = System.currentTimeMillis()
        eventStore.flushOnLaunch()
        val now = System.currentTimeMillis()
        assertTrue(now - baseline > 1500)
        backgroundTaskService.shutdown()
        assertEquals(1, delivery.count.get())
    }

    /**
     * Crashes on launch are delivered synchronously
     */
    @Test
    fun flushOnLaunchSync() {
        val delivery = TestDelivery()
        val backgroundTaskService = BackgroundTaskService()
        val eventStore = createEventStore(delivery, backgroundTaskService)

        // launch crashes are delivered in flushOnLaunch()
        val event = generateEvent()
        event.app.isLaunching = true
        eventStore.write(event)
        eventStore.flushOnLaunch()
        assertEquals(1, delivery.count.get())
        assertEquals("Bugsnag Error thread", delivery.threadName)

        // non-launch crashes are not delivered in flushOnLaunch()
        event.app.isLaunching = false
        eventStore.write(event)
        eventStore.flushOnLaunch()
        assertEquals(1, delivery.count.get())

        // non-launch crashes are delivered in flushAsync() instead
        eventStore.flushAsync()
        backgroundTaskService.shutdown()
        assertEquals(2, delivery.count.get())
        assertEquals("Bugsnag Error thread", delivery.threadName)
    }

    /**
     * Only the most recent crash report is sent by flushOnLaunch()
     */
    @Test
    fun flushOnLaunchSendsMostRecent() {
        val delivery = TestDelivery()
        val backgroundTaskService = BackgroundTaskService()
        val eventStore = createEventStore(delivery, backgroundTaskService)

        // launch crashes are delivered in flushOnLaunch()
        val event = generateEvent()
        event.app.isLaunching = true
        event.apiKey = "First"
        eventStore.write(event)
        event.apiKey = "Second"
        eventStore.write(event)

        // only the first event should be sent
        eventStore.flushOnLaunch()
        assertEquals(1, delivery.count.get())
        val payload = requireNotNull(delivery.payload)
        val filenameInfo = EventFilenameInfo.fromFile(
            requireNotNull(payload.eventFile),
            BugsnagTestUtils.generateImmutableConfig()
        )
        assertEquals("Second", filenameInfo.apiKey)
    }

    private class TestDelivery : Delivery {
        var threadName: String? = null
        val count = AtomicInteger(0)
        var payload: EventPayload? = null

        override fun deliver(payload: Session, deliveryParams: DeliveryParams) =
            DeliveryStatus.DELIVERED

        override fun deliver(
            payload: EventPayload,
            deliveryParams: DeliveryParams
        ): DeliveryStatus {
            // capture thread on which executor was running
            threadName = Thread.currentThread().name
            count.getAndIncrement()
            this.payload = payload
            return DeliveryStatus.DELIVERED
        }
    }

    private class SlowDelivery : Delivery {
        val count = AtomicInteger(0)

        override fun deliver(payload: Session, deliveryParams: DeliveryParams) =
            DeliveryStatus.DELIVERED

        override fun deliver(
            payload: EventPayload,
            deliveryParams: DeliveryParams
        ): DeliveryStatus {
            Thread.sleep(2000)
            count.getAndIncrement()
            return DeliveryStatus.DELIVERED
        }
    }

    private fun createEventStore(
        testDelivery: Delivery,
        backgroundTaskService: BackgroundTaskService
    ): EventStore {
        val config = generateConfiguration().apply {
            persistenceDirectory = storageDir
            this.delivery = testDelivery
        }
        return EventStore(
            BugsnagTestUtils.convert(config),
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
