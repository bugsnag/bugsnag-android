package com.bugsnag.android

import android.content.Context
import android.net.ConnectivityManager

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
internal fun createSlowDelivery(context: Context): Delivery {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?
    val delivery = DefaultDelivery(cm)

    return object : Delivery {
        override fun deliver(payload: SessionTrackingPayload?, config: Configuration?) {
            Thread.sleep(500)
            delivery.deliver(payload, config)
        }

        override fun deliver(report: Report?, config: Configuration?) {
            Thread.sleep(500)
            delivery.deliver(report, config)
        }
    }
}

internal fun createDefaultDelivery(context: Context): DefaultDelivery {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?
    return DefaultDelivery(cm)
}

internal fun createCustomHeaderDelivery(context: Context): Delivery {
    return object : Delivery {
        val delivery: DefaultDelivery = createDefaultDelivery(context)

        override fun deliver(payload: SessionTrackingPayload?, config: Configuration?) {
            deliver(config?.sessionEndpoint, payload, config?.sessionApiHeaders)
        }

        override fun deliver(report: Report?, config: Configuration?) {
            deliver(config?.endpoint, report, config?.errorApiHeaders)
        }

        fun deliver(endpoint: String?,
                    streamable: JsonStream.Streamable?,
                    headers: MutableMap<String, String>?) {
            headers!!["Custom-Client"] = "Hello World"
            delivery.deliver(endpoint, streamable, headers)
        }
    }
}


internal fun writeErrorToStore(client: Client) {
    val error = Error.Builder(Configuration("api-key"), RuntimeException(), null).build()
    client.errorStore.write(error)
}
