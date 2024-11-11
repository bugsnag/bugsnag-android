package com.bugsnag.android.okhttp

import android.net.TrafficStats
import com.bugsnag.android.Delivery
import com.bugsnag.android.DeliveryParams
import com.bugsnag.android.DeliveryStatus
import com.bugsnag.android.EventPayload
import com.bugsnag.android.Logger
import com.bugsnag.android.Session
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

private const val BUGSNAG_INTEGRITY_HEADER = "Bugsnag-Integrity"

class OkHttpDelivery @JvmOverloads constructor(
    private val client: OkHttpClient = OkHttpClient.Builder().build(),
    private val logger: Logger? = null,
) : Delivery {
    override fun deliver(payload: Session, deliveryParams: DeliveryParams): DeliveryStatus {
        TrafficStats.setThreadStatsTag(1)
        try {
            val requestBody = payload.toByteArray().toRequestBody()
            val integrityHeader = payload.integrityToken

            val requestBuilder = Request.Builder()
                .url(deliveryParams.endpoint)
                .headers(deliveryParams.toHeaders())
                .post(requestBody)

            if (integrityHeader != null) {
                requestBuilder.header(BUGSNAG_INTEGRITY_HEADER, integrityHeader)
            }

            val call = client.newCall(requestBuilder.build())
            val response = call.execute()

            return DeliveryStatus.forHttpResponseCode(response.code)
        } finally {
            TrafficStats.clearThreadStatsTag()
        }
    }

    override fun deliver(payload: EventPayload, deliveryParams: DeliveryParams): DeliveryStatus {
        TrafficStats.setThreadStatsTag(1)
        try {
            val requestBody = payload.trimToSize().toByteArray().toRequestBody()
            val integrityHeader = payload.integrityToken

            val requestBuilder = Request.Builder()
                .url(deliveryParams.endpoint)
                .headers(deliveryParams.toHeaders())
                .post(requestBody)

            if (integrityHeader != null) {
                requestBuilder.header(BUGSNAG_INTEGRITY_HEADER, integrityHeader)
            }

            val call = client.newCall(requestBuilder.build())

            val response = call.execute()
            return DeliveryStatus.forHttpResponseCode(response.code)
        } catch (oom: OutOfMemoryError) {
            // attempt to persist the payload on disk. This approach uses streams to write to a
            // file, which takes less memory than serializing the payload into a ByteArray, and
            // therefore has a reasonable chance of retaining the payload for future delivery.
            logger?.w("Encountered OOM delivering payload, falling back to persist on disk", oom)
            return DeliveryStatus.UNDELIVERED
        } catch (exception: IOException) {
            logger?.w("IOException encountered in request", exception)
            return DeliveryStatus.UNDELIVERED
        } catch (exception: Exception) {
            logger?.w("Unexpected error delivering payload", exception)
            return DeliveryStatus.FAILURE
        } finally {
            TrafficStats.clearThreadStatsTag()
        }
    }

    private fun DeliveryParams.toHeaders(): Headers {
        return Headers.Builder().run {
            headers.forEach { (name, value) ->
                value?.let { add(name, it) }
            }

            build()
        }
    }
}
