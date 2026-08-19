package com.bugsnag.android

import com.bugsnag.android.BugsnagTestUtils.generateImmutableConfig
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import java.util.Date

@RunWith(MockitoJUnitRunner::class)
internal class EventDeliveryStrategyTest {

    private val apiKey = "BUGSNAG_API_KEY"
    private val notifier = Notifier()
    private val config = generateImmutableConfig()
    private val testException = RuntimeException("Example message")

    @Test
    fun testGenerateUnhandledReport() {
        val event = Event(
            RuntimeException("Whoops!"), config,
            SeverityReason.newInstance(
                SeverityReason.REASON_UNHANDLED_EXCEPTION
            ),
            NoopLogger
        )
        event.session = Session(
            "123", Date(), User(null, null, null), false, notifier, NoopLogger, apiKey
        )
        assertEquals(DeliveryStrategy.STORE_ONLY, event.deliveryStrategy)
        event.deliveryStrategy = DeliveryStrategy.SEND_IMMEDIATELY
        assertEquals(DeliveryStrategy.SEND_IMMEDIATELY, event.deliveryStrategy)
    }

    @Test
    fun testANRStoreEventAndFlush() {
        val anrEvent = Event(
            testException, config,
            SeverityReason.newInstance(SeverityReason.REASON_ANR),
            NoopLogger
        )
        anrEvent.getErrors().get(0).setErrorClass("ANR")
        assertEquals(DeliveryStrategy.STORE_AND_FLUSH, anrEvent.getDeliveryStrategy())
    }

    @Test
    fun testPromiseRejectionEvetStoreAndFlush() {
        val promiseRejectionEvent = Event(
            testException, config,
            SeverityReason.newInstance(SeverityReason.REASON_PROMISE_REJECTION),
            NoopLogger
        )
        assertEquals(DeliveryStrategy.STORE_AND_FLUSH, promiseRejectionEvent.getDeliveryStrategy())
    }

    @Test
    fun testANRWithModifiedErrorClassStoreAndFlush() {
        val modifiedAnrEvent = Event(
            testException,
            config,
            SeverityReason.newInstance(SeverityReason.REASON_PROMISE_REJECTION),
            NoopLogger
        )
        modifiedAnrEvent.getErrors().get(0).setErrorClass("ANR")
        assertEquals(DeliveryStrategy.STORE_AND_FLUSH, modifiedAnrEvent.getDeliveryStrategy())
    }

    @Test
    fun testGenerateHandledReport() {
        val state = SeverityReason.newInstance(
            SeverityReason.REASON_HANDLED_EXCEPTION
        )
        val event = Event(RuntimeException("Whoops!"), config, state, NoopLogger)
        event.session = Session(
            "123", Date(), User(null, null, null), false, notifier, NoopLogger, apiKey
        )
        assertEquals(DeliveryStrategy.SEND_IMMEDIATELY, event.deliveryStrategy)
    }

    @Test
    fun testDeliveryStrategyStoreAndSend() {
        val configuration = Configuration(apiKey)
        val newConfig = generateImmutableConfig(configuration).apply {
            configuration.setAttemptDeliveryOnCrash(true)
        }
        val unhandledEvent = Event(
            testException, newConfig,
            SeverityReason.newInstance(SeverityReason.REASON_UNHANDLED_EXCEPTION),
            NoopLogger
        )
        assertEquals(DeliveryStrategy.STORE_ONLY, unhandledEvent.getDeliveryStrategy())
    }
}
