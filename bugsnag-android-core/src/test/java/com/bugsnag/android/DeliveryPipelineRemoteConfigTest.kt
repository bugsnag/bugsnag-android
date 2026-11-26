package com.bugsnag.android

import com.bugsnag.android.BugsnagTestUtils.generateConfiguration
import com.bugsnag.android.BugsnagTestUtils.generateEventPayload
import com.bugsnag.android.internal.DeliveryPipeline
import com.bugsnag.android.internal.ImmutableConfig
import com.bugsnag.android.internal.remoteconfig.RemoteConfigState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.util.Date
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class DeliveryPipelineRemoteConfigTest {

    private lateinit var remoteConfigState: RemoteConfigState
    private lateinit var config: ImmutableConfig
    private val deliveredCount = AtomicInteger()

    @Before
    fun setUp() {
        remoteConfigState = mock(RemoteConfigState::class.java)
        config = BugsnagTestUtils.convert(
            generateConfiguration().apply {
                delivery = object : Delivery {
                    override fun deliver(payload: Session, deliveryParams: DeliveryParams) =
                        DeliveryStatus.DELIVERED

                    override fun deliver(payload: EventPayload, deliveryParams: DeliveryParams): DeliveryStatus {
                        deliveredCount.incrementAndGet()
                        return DeliveryStatus.DELIVERED
                    }
                }
            }
        )
    }

    @Test
    fun deliveryTriggersRefreshAndNeverWaitsForConfig() {
        `when`(remoteConfigState.peekRemoteConfig()).thenReturn(null)

        val status = pipeline().deliverEventPayload(generateEventPayload(config))

        assertEquals(DeliveryStatus.DELIVERED, status)
        assertEquals(1, deliveredCount.get())
        verify(remoteConfigState).scheduleDownloadIfRequired()
        verify(remoteConfigState).peekRemoteConfig()
        verify(remoteConfigState, never()).getRemoteConfig(anyLong(), any(TimeUnit::class.java))
        verify(remoteConfigState, never()).getRemoteConfig()
    }

    @Test
    fun cachedDiscardRulesAreApplied() {
        `when`(remoteConfigState.peekRemoteConfig()).thenReturn(
            RemoteConfig("tag", Date(System.currentTimeMillis() + 60_000), listOf(DiscardRule.All))
        )

        val status = pipeline().deliverEventPayload(generateEventPayload(config))

        assertEquals(DeliveryStatus.DELIVERED, status)
        assertEquals("discarded events must not reach the Delivery", 0, deliveredCount.get())
        verify(remoteConfigState).scheduleDownloadIfRequired()
    }

    @Test
    fun refreshIsTriggeredEvenWhenOnSendDropsTheEvent() {
        val callbackState = CallbackState().apply { addOnSend(OnSendCallback { false }) }

        val status = DeliveryPipeline(callbackState, remoteConfigState, config)
            .deliverEventPayload(generateEventPayload(config))

        assertNull(status)
        assertEquals(0, deliveredCount.get())
        verify(remoteConfigState).scheduleDownloadIfRequired()
    }

    @Test
    fun deliveryContinuesWhenRefreshSchedulingFails() {
        doThrow(IllegalStateException("boom")).`when`(remoteConfigState).scheduleDownloadIfRequired()

        val status = pipeline().deliverEventPayload(generateEventPayload(config))

        assertEquals(DeliveryStatus.DELIVERED, status)
        assertEquals(1, deliveredCount.get())
    }

    private fun pipeline() = DeliveryPipeline(CallbackState(), remoteConfigState, config)
}
