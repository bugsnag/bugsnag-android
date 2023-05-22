package com.bugsnag.android

import com.bugsnag.android.BugsnagTestUtils.generateImmutableConfig
import com.bugsnag.android.internal.BackgroundTaskService
import com.bugsnag.android.internal.StateObserver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import java.util.Date

@RunWith(MockitoJUnitRunner::class)
internal class DeliveryDelegateTest {

    @Mock
    lateinit var eventStore: EventStore

    private val apiKey = "BUGSNAG_API_KEY"
    private val notifier = Notifier()
    val config = generateImmutableConfig()
    val callbackState = CallbackState()
    private val logger = InterceptingLogger()
    lateinit var deliveryDelegate: DeliveryDelegate
    val handledState = SeverityReason.newInstance(
        SeverityReason.REASON_UNHANDLED_EXCEPTION
    )
    val event = Event(RuntimeException("Whoops!"), config, handledState, NoopLogger)

    @Before
    fun setUp() {
        deliveryDelegate =
            DeliveryDelegate(
                logger,
                eventStore,
                config,
                callbackState,
                notifier,
                BackgroundTaskService()
            )
        event.session = Session("123", Date(), User(null, null, null), false, notifier, NoopLogger, apiKey)
    }

    @Test
    fun generateUnhandledReport() {
        var msg: StateEvent.NotifyUnhandled? = null
        deliveryDelegate.addObserver(
            StateObserver {
                msg = it as StateEvent.NotifyUnhandled
            }
        )
        deliveryDelegate.deliver(event)

        // verify message sent
        assertNotNull(msg)

        // check session count incremented
        assertEquals(1, event.session!!.unhandledCount)
        assertEquals(0, event.session!!.handledCount)

        assertEquals("BUGSNAG_API_KEY", event.session!!.apiKey)
    }

    @Test
    fun generateHandledReport() {
        val state = SeverityReason.newInstance(
            SeverityReason.REASON_HANDLED_EXCEPTION
        )
        val event = Event(RuntimeException("Whoops!"), config, state, NoopLogger)
        event.session = Session("123", Date(), User(null, null, null), false, notifier, NoopLogger, apiKey)

        var msg: StateEvent.NotifyHandled? = null
        deliveryDelegate.addObserver(
            StateObserver {
                msg = it as StateEvent.NotifyHandled
            }
        )
        deliveryDelegate.deliver(event)

        // verify message sent
        assertNotNull(msg)

        // check session count incremented
        assertEquals(0, event.session!!.unhandledCount)
        assertEquals(1, event.session!!.handledCount)
    }

    @Test
    fun generateEmptyReport() {
        val state = SeverityReason.newInstance(
            SeverityReason.REASON_HANDLED_EXCEPTION
        )
        val event = Event(RuntimeException("Whoops!"), config, state, NoopLogger)
        event.errors.clear()

        var msg: StateEvent.NotifyHandled? = null
        deliveryDelegate.addObserver(
            StateObserver {
                msg = it as StateEvent.NotifyHandled
            }
        )
        deliveryDelegate.deliver(event)

        // verify no payload was sent for an Event with no errors
        assertNull(msg)
    }

    @Test
    fun deliverReport() {
        val eventPayload = EventPayload("api-key", event, null, notifier, config)
        val status = deliveryDelegate.deliverPayloadInternal(eventPayload, event)
        assertEquals(DeliveryStatus.DELIVERED, status)
        assertEquals("Sent 1 new event to Bugsnag", logger.msg)
    }

    private class InterceptingLogger : Logger {
        var msg: String? = null
        override fun i(msg: String) {
            this.msg = msg
        }
    }
}
