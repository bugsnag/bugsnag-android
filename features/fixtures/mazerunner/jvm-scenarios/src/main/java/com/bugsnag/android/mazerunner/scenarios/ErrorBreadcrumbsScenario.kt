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

    init {
        val baseDelivery = createDefaultDelivery()
        config.delivery = object : Delivery {
            override fun deliver(payload: EventPayload, deliveryParams: DeliveryParams): DeliveryStatus {
                if (payload.event?.isUnhandled == false) {
                    // Keep the first handled error in-flight long enough for the crash to persist it.
                    Thread.sleep(5_000)
                }

                return baseDelivery.deliver(payload, deliveryParams)
            }

            override fun deliver(payload: Session, deliveryParams: DeliveryParams): DeliveryStatus {
                return baseDelivery.deliver(payload, deliveryParams)
            }
        }
    }

    override fun startScenario() {
        Bugsnag.notify(RuntimeException("first error"))
        // Make sure the handled error file exists before the crash happens.
        waitForEventFile()
        throw NullPointerException("something broke")
    }
}
