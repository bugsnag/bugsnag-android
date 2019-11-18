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
internal fun createSlowDelivery(context: Context): Delivery {
    val delivery = DefaultDelivery(null)

    return object : Delivery {
        override fun deliver(payload: SessionPayload, config: Configuration) {
            Thread.sleep(500)
            delivery.deliver(payload, config)
        }

        override fun deliver(report: Report, config: Configuration) {
            Thread.sleep(500)
            delivery.deliver(report, config)
        }
    }
}

internal fun createDefaultDelivery(context: Context): DefaultDelivery {
    return DefaultDelivery(null)
}

internal fun createCustomHeaderDelivery(context: Context): Delivery {
    return object : Delivery {
        val delivery: DefaultDelivery = createDefaultDelivery(context)

        override fun deliver(payload: SessionPayload, config: Configuration) {
            deliver(config.sessionEndpoint, payload, config.sessionApiHeaders)
        }

        override fun deliver(report: Report, config: Configuration) {
            deliver(config.endpoint, report, config.errorApiHeaders)
        }

        fun deliver(endpoint: String,
                    streamable: JsonStream.Streamable,
                    headers: MutableMap<String, String>) {
            headers["Custom-Client"] = "Hello World"
            delivery.deliver(endpoint, streamable, headers)
        }
    }
}


internal fun writeErrorToStore(client: Client) {
    val error = Error.Builder(Configuration("api-key"), RuntimeException(), null,
        Thread.currentThread(), false).build()
    client.errorStore.write(error)
}

internal fun sendInternalReport(exc: Throwable, config: Configuration, client: Client) {
    val thread = Thread.currentThread()
    val err = Error.Builder(config, exc, null, thread, true).build()
    err.getMetaData().addToTab("BugsnagDiagnostics", "custom-data", "FooBar")
    client.reportInternalBugsnagError(err)
}
