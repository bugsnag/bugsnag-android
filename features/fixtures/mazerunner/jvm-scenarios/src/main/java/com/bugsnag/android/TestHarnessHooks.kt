package com.bugsnag.android

import com.bugsnag.android.JavaHooks.generateAppWithState
import com.bugsnag.android.JavaHooks.generateDeviceWithState
import java.lang.Thread

/**
 * Creates a delivery API client with a 500ms delay, emulating poor network connectivity
 */
internal fun createSlowDelivery(): Delivery {
    val delivery = createDefaultDelivery()

    return object : Delivery {
        override fun deliver(
            payload: EventPayload,
            deliveryParams: DeliveryParams
        ): DeliveryStatus {
            Thread.sleep(500)
            return delivery.deliver(payload, deliveryParams)
        }

        override fun deliver(payload: Session, deliveryParams: DeliveryParams): DeliveryStatus {
            Thread.sleep(500)
            return delivery.deliver(payload, deliveryParams)
        }
    }
}

internal fun createCustomHeaderDelivery(): Delivery {
    val delivery = createDefaultDelivery()

    return object : Delivery {
        override fun deliver(payload: Session, deliveryParams: DeliveryParams): DeliveryStatus {
            return delivery.deliver(payload, mutateDeliveryParams(deliveryParams))
        }

        override fun deliver(payload: EventPayload, deliveryParams: DeliveryParams): DeliveryStatus {
            return delivery.deliver(payload, mutateDeliveryParams(deliveryParams))
        }

        fun mutateDeliveryParams(params: DeliveryParams): DeliveryParams {
            val map = params.headers.toMutableMap()
            map["Custom-Client"] = "Hello World"
            return DeliveryParams(params.endpoint, map.toMap())
        }
    }
}

fun createDefaultDelivery(): Delivery = JavaHooks.createDefaultDelivery()

fun generateEvent(client: Client): Event {
    val event = NativeInterface.createEvent(
        RuntimeException(),
        client,
        SeverityReason.newInstance(SeverityReason.REASON_ANR)
    )
    event.app = generateAppWithState(client.config)
    event.device = generateDeviceWithState()
    return event
}

fun setAutoNotify(client: Client, enabled: Boolean) {
    client.setAutoNotify(enabled)
}

fun setAutoDetectAnrs(client: Client, enabled: Boolean) {
    client.setAutoDetectAnrs(enabled)
}
