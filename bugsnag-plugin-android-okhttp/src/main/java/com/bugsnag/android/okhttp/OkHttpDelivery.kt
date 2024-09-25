package com.bugsnag.android.okhttp

import android.annotation.SuppressLint
import com.bugsnag.android.Configuration
import com.bugsnag.android.Delivery
import com.bugsnag.android.DeliveryParams
import com.bugsnag.android.DeliveryStatus
import com.bugsnag.android.EventPayload
import com.bugsnag.android.Logger
import com.bugsnag.android.SerializePayload.serializePayload
import com.bugsnag.android.Session
import com.bugsnag.android.internal.JsonHelper
import okhttp3.Headers
import okhttp3.Headers.Companion.toHeaders
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

class OkHttpDelivery constructor(
    private val existingOkHttpClient: OkHttpClient,
    private val apiKey: String,
    private val maxStringValueLength: Int,
    private val logger: Logger
) : Delivery {

    companion object {
        @SuppressLint("NotConstructor")
        @JvmSynthetic
        fun Configuration.OkHttpDelivery(okHttpClient: OkHttpClient): OkHttpDelivery {
            return OkHttpDelivery(okHttpClient, apiKey, maxStringValueLength, logger!!)
        }

        fun Configuration.useOkHttpDelivery(okHttpClient: OkHttpClient) {
            delivery = OkHttpDelivery(okHttpClient)
        }
    }

    override fun deliver(payload: EventPayload, deliveryParams: DeliveryParams): DeliveryStatus {
        val json = serializePayload(payload, apiKey, maxStringValueLength, logger)
        val status = makeRequest(deliveryParams.endpoint, json, deliveryParams.headers)
        logger.i("Error API request finished with status $status")
        return status
    }

    override fun deliver(payload: Session, deliveryParams: DeliveryParams): DeliveryStatus {
        val status = makeRequest(deliveryParams.endpoint, JsonHelper.serialize(payload), deliveryParams.headers)
        logger.i("Session API request finished with status $status")
        return status
    }

    private fun makeRequest(
        url: String,
        json: ByteArray,
        headers: Map<String, String?>
    ): DeliveryStatus {
        return try {
            val mediaType = "application/json".toMediaTypeOrNull()
            val headersNotNull = convertHeaders(headers)
            val request = Request.Builder()
                .headers(headersNotNull.toHeaders()) // add error API headers
                .url(url) // use error API endpoint
                .post(json.toRequestBody(mediaType))
                .build()
            val response = existingOkHttpClient.newCall(request).execute()
            logRequestInfo(response.code, response.message, response.headers)
            getDeliveryStatus(response.code)
        } catch (oom: OutOfMemoryError) {
            logger.w("Encountered OOM delivering payload, falling back to persist on disk", oom)
            return DeliveryStatus.UNDELIVERED
        } catch (exception: IOException) {
            logger.w("IOException encountered in request", exception)
            return DeliveryStatus.UNDELIVERED
        } catch (exception: Exception) {
            logger.w("Unexpected error delivering payload", exception)
            return DeliveryStatus.FAILURE
        }
    }

    private fun logRequestInfo(code: Int, message: String, headers: Headers) {
        runCatching {
            logger.i(
                "Request completed with code $code, " +
                    "message: $message, " +
                    "headers: $headers"
            )
        }
    }

    private fun convertHeaders(headers: Map<String, String?>): Map<String, String> {
        return headers.filterValues { it != null }.mapValues { it.value as String }
    }

    @Suppress("MagicNumber")
    private fun getDeliveryStatus(responseCode: Int): DeliveryStatus {
        return when {
            responseCode in 200..299 -> DeliveryStatus.DELIVERED
            isUnrecoverableStatusCode(responseCode) -> DeliveryStatus.FAILURE
            else -> DeliveryStatus.UNDELIVERED
        }
    }

    @Suppress("MagicNumber")
    private fun isUnrecoverableStatusCode(responseCode: Int) =
        responseCode in 400..499 && // 400-499 are considered unrecoverable
            responseCode != 408 && // except for 408
            responseCode != 429 // and 429
}
