package com.bugsnag.android

import com.bugsnag.android.JavaHooks.generateAppWithState
import com.bugsnag.android.JavaHooks.generateDeviceWithState
import java.lang.Thread

internal fun triggerInternalBugsnagForError(client: Client) {
    client.eventStore.write {
        throw IllegalStateException("Mazerunner threw exception serializing error")
    }
}

internal fun flushErrorStoreAsync(client: Client) {
    client.eventStore.flushAsync()
}

internal fun flushErrorStoreOnLaunch(client: Client) {
    client.eventStore.flushOnLaunch()
}

/**
 * Creates a delivery API client with a 500ms delay, emulating poor network connectivity
 */
internal fun createSlowDelivery(): Delivery {
    val delivery = createDefaultDelivery()

    return object : Delivery {
        override fun deliver(payload: EventPayload, deliveryParams: DeliveryParams): DeliveryStatus {
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

fun createDefaultDelivery(): Delivery { // use reflection as DefaultDelivery is internal
    val clz = Class.forName("com.bugsnag.android.DefaultDelivery")
    return clz.constructors[0].newInstance(
        null,
        object : Logger {
            override fun e(msg: String) = Unit
            override fun e(msg: String, throwable: Throwable) = Unit
            override fun w(msg: String) = Unit
            override fun w(msg: String, throwable: Throwable) = Unit
            override fun i(msg: String) = Unit
            override fun i(msg: String, throwable: Throwable) = Unit
            override fun d(msg: String) = Unit
            override fun d(msg: String, throwable: Throwable) = Unit
        }
    ) as Delivery
}

internal fun writeErrorToStore(client: Client, event: Event) {
    client.eventStore.write(event)
}

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
