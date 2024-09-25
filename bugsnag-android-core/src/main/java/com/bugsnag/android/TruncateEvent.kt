package com.bugsnag.android

import com.bugsnag.android.internal.JsonHelper

internal fun truncateEvent(
    payload: EventPayload,
    apiKey: String,
    maxStringValueLength: Int,
    logger: Logger
): ByteArray {
    var event = payload.event
    if (event == null) {
        event = MarshalledEventSource(payload.eventFile!!, apiKey, logger).invoke()
        payload.event = event
        payload.apiKey = apiKey
    }

    val (itemsTrimmed, dataTrimmed) = event.impl.trimMetadataStringsTo(maxStringValueLength)
    event.impl.internalMetrics.setMetadataTrimMetrics(
        itemsTrimmed,
        dataTrimmed
    )

    val json = JsonHelper.serialize(payload)
    if (json.size <= DefaultDelivery.maxPayloadSize) {
        return json
    }

    val breadcrumbAndBytesRemovedCounts =
        event.impl.trimBreadcrumbsBy(json.size - DefaultDelivery.maxPayloadSize)
    event.impl.internalMetrics.setBreadcrumbTrimMetrics(
        breadcrumbAndBytesRemovedCounts.itemsTrimmed,
        breadcrumbAndBytesRemovedCounts.dataTrimmed
    )
    return JsonHelper.serialize(payload)
}
