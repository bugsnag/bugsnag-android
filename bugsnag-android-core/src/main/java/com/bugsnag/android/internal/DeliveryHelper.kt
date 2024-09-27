package com.bugsnag.android.internal

import com.bugsnag.android.DefaultDelivery
import com.bugsnag.android.DeliveryStatus
import com.bugsnag.android.EventPayload
import com.bugsnag.android.Logger
import com.bugsnag.android.computeSha1Digest
import com.bugsnag.android.truncateEvent
import java.net.HttpURLConnection.HTTP_BAD_REQUEST
import java.net.HttpURLConnection.HTTP_CLIENT_TIMEOUT
import java.net.HttpURLConnection.HTTP_OK

object DeliveryHelper {
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

    fun getDeliveryStatus(responseCode: Int): DeliveryStatus {
        return when {
            responseCode in HTTP_OK..299 -> DeliveryStatus.DELIVERED
            isUnrecoverableStatusCode(responseCode) -> DeliveryStatus.FAILURE
            else -> DeliveryStatus.UNDELIVERED
        }
    }

    fun isUnrecoverableStatusCode(responseCode: Int) =
        responseCode in HTTP_BAD_REQUEST..499 && // 400-499 are considered unrecoverable
            responseCode != HTTP_CLIENT_TIMEOUT && // except for 408
            responseCode != 429 // and 429

    fun retrieveSha1Digest(payload: ByteArray): String? {
        return computeSha1Digest(payload)
    }
}
