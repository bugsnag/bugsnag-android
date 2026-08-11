package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.BreadcrumbType
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration
import com.bugsnag.android.Delivery
import com.bugsnag.android.DeliveryParams
import com.bugsnag.android.DeliveryStatus
import com.bugsnag.android.EventPayload
import com.bugsnag.android.Session
import com.bugsnag.android.createDefaultDelivery
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class ErrorBreadcrumbsScenario(
    config: Configuration,
    context: Context,
    eventMetadata: String?
) : Scenario(
    config.apply {
        enabledBreadcrumbTypes = setOf(BreadcrumbType.ERROR)
    },
    context,
    eventMetadata
) {

    private companion object {
        const val HANDLED_ERROR_TIMEOUT_SECONDS = 10L
    }

    private val handledErrorDelivered = AtomicBoolean(false)
    private val handledDeliveryCompleted = CountDownLatch(1)

    init {
        val baseDelivery = createDefaultDelivery()
        config.delivery = object : Delivery {
            override fun deliver(payload: EventPayload, deliveryParams: DeliveryParams): DeliveryStatus {
                val status = baseDelivery.deliver(payload, deliveryParams)

                if (payload.event?.isUnhandled == false && handledErrorDelivered.compareAndSet(false, true)) {
                    handledDeliveryCompleted.countDown()
                }

                return status
            }

            override fun deliver(payload: Session, deliveryParams: DeliveryParams): DeliveryStatus {
                return baseDelivery.deliver(payload, deliveryParams)
            }
        }
    }

    override fun startScenario() {
        Bugsnag.notify(RuntimeException("first error"))
        handledDeliveryCompleted.await(HANDLED_ERROR_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        throw NullPointerException("something broke")
    }
}
