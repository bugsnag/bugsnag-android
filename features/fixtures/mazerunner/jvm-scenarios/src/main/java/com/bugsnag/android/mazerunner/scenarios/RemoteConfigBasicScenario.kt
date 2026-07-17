package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration
import com.bugsnag.android.Delivery
import com.bugsnag.android.DeliveryParams
import com.bugsnag.android.DeliveryStatus
import com.bugsnag.android.EndpointConfiguration
import com.bugsnag.android.EventPayload
import com.bugsnag.android.Session
import com.bugsnag.android.createDefaultDelivery
import java.io.IOException

class RemoteConfigBasicScenario(
    config: Configuration,
    context: Context,
    eventMetadata: String
) : Scenario(config, context, eventMetadata) {
    companion object {
        private const val UNHANDLED_DELAY_MS = 3000L
    }

    private val handler = Handler(Looper.getMainLooper())
    private var deliveredHandledError = false

    init {
        val baseDelivery = createDefaultDelivery()
        config.delivery = object : Delivery {
            override fun deliver(payload: EventPayload, deliveryParams: DeliveryParams): DeliveryStatus {
                val status = baseDelivery.deliver(payload, deliveryParams)
                check(status == DeliveryStatus.DELIVERED) {
                    "Request failed, aborting scenario. status=$status"
                }

                if (!deliveredHandledError && payload.event?.isUnhandled == false) {
                    deliveredHandledError = true
                    handler.postDelayed({ throw IOException("Unhandled exception") }, UNHANDLED_DELAY_MS)
                }

                return status
            }

            override fun deliver(payload: Session, deliveryParams: DeliveryParams): DeliveryStatus {
                return baseDelivery.deliver(payload, deliveryParams)
            }
        }

        if (eventMetadata == "disable-remote-config") {
            config.endpoints = EndpointConfiguration(
                config.endpoints.notify,
                config.endpoints.sessions
            )
        }
    }

    override fun startScenario() {
        super.startScenario()
        Bugsnag.notify(RuntimeException("Handled exception"))
    }
}
