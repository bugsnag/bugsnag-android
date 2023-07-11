package com.bugsnag.android

import android.content.Context
import java.util.Date

internal fun generateClient(ctx: Context, cfg: Configuration = generateConfig()): Client {
    return Client(ctx, cfg)
}

fun generateConfig(): Configuration {
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


internal fun generateSession(): Session {
    return Session(
        "test",
        Date(),
        null,
        false,
        Notifier(),
        object : Logger {},
        null
    )
}
