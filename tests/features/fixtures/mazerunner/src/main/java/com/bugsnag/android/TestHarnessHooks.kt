package com.bugsnag.android

import android.content.Context

/**
 * Accesses the session tracker and flushes all stored sessions
 */
internal fun flushAllSessions() {
    Bugsnag.getClient().sessionTracker.flushStoredSessions()
}

internal fun flushErrorStoreAsync(client: Client) {
    client.errorStore.flushAsync()
}

internal fun flushErrorStoreOnLaunch(client: Client) {
    client.errorStore.flushOnLaunch()
}

/**
 * Creates a delivery API client with a 500ms delay, emulating poor network connectivity
 */
internal fun createSlowDelivery(config: Configuration): Delivery {
    val delivery = createDefaultDelivery()

    return object : Delivery {
        override fun deliver(report: Report, deliveryParams: DeliveryParams): DeliveryStatus {
            Thread.sleep(500)
            return delivery.deliver(report, deliveryParams)
        }

        override fun deliver(payload: SessionTrackingPayload, deliveryParams: DeliveryParams): DeliveryStatus {
            Thread.sleep(500)
            return delivery.deliver(payload, deliveryParams)
        }
    }
}

internal fun createCustomHeaderDelivery(config: Configuration): Delivery {
    val delivery = createDefaultDelivery()

    return object : Delivery {
        override fun deliver(payload: SessionTrackingPayload, deliveryParams: DeliveryParams): DeliveryStatus {
            return delivery.deliver(payload, mutateDeliveryParams(deliveryParams))
        }

        override fun deliver(report: Report, deliveryParams: DeliveryParams): DeliveryStatus {
            return delivery.deliver(report, mutateDeliveryParams(deliveryParams))
        }

        fun mutateDeliveryParams(params: DeliveryParams): DeliveryParams {
            val map = params.headers.toMutableMap()
            map["Custom-Client"] = "Hello World"
            return DeliveryParams(params.endpoint, map.toMap())
        }
    }
}

internal fun createDefaultDelivery(): Delivery { // use reflection as DefaultDelivery is internal
    val clz = java.lang.Class.forName("com.bugsnag.android.DefaultDelivery")
    return clz.constructors[0].newInstance(null) as Delivery
}

internal fun writeErrorToStore(client: Client) {
    val error = Error.Builder(Configuration("api-key"), RuntimeException(), null,
        Thread.currentThread(), false).build()
    client.errorStore.write(error)
}
