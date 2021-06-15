package com.bugsnag.android

import android.content.Context

internal fun generateClient(ctx: Context, cfg: Configuration = generateConfig()): Client {
    return Client(ctx, cfg)
}

internal fun generateConfig(): Configuration {
    return Configuration("your-api-key").apply {
        // logging is disabled by default in production apps
        logger = object : Logger {}

        // avoid making network requests in performance tests
        delivery = object : Delivery {
            override fun deliver(
                payload: Session,
                deliveryParams: DeliveryParams
            ) = DeliveryStatus.DELIVERED

            override fun deliver(
                payload: EventPayload,
                deliveryParams: DeliveryParams
            ) = DeliveryStatus.DELIVERED
        }
    }
}

internal fun generateSeverityReason() =
    SeverityReason.newInstance(SeverityReason.REASON_UNHANDLED_EXCEPTION)
