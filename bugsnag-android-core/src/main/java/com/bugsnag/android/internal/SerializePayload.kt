package com.bugsnag.android.internal

import com.bugsnag.android.DefaultDelivery
import com.bugsnag.android.EventPayload
import com.bugsnag.android.Logger
import com.bugsnag.android.truncateEvent

fun serializePayload(
    payload: EventPayload,
    apiKey: String,
    maxStringValueLength: Int,
    logger: Logger
): ByteArray {
    var json = JsonHelper.serialize(payload)
    if (json.size <= DefaultDelivery.maxPayloadSize) {
        return json
    }
    return truncateEvent(payload, apiKey, maxStringValueLength, logger)
}
