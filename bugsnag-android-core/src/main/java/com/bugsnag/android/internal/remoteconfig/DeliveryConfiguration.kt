package com.bugsnag.android.internal.remoteconfig

import com.bugsnag.android.DeliveryParams

internal class DeliveryConfiguration(
    val payloadEncoding: DeliveryParams.PayloadEncoding,
) {
    companion object {
        internal const val KEY_PAYLOAD_ENCODING = "payloadEncoding"

        internal const val ENCODING_NONE = "none"
        internal const val ENCODING_GZIP = "gzip"

        fun fromJsonMap(json: Map<String, *>): DeliveryConfiguration? {
            val payloadEncodingName = json[KEY_PAYLOAD_ENCODING] as? String
                ?: return null

            val payloadEncoding = when (payloadEncodingName) {
                ENCODING_GZIP -> DeliveryParams.PayloadEncoding.GZIP
                else -> DeliveryParams.PayloadEncoding.NONE
            }

            return DeliveryConfiguration(payloadEncoding)
        }
    }
}
