package com.bugsnag.android

import android.net.TrafficStats
import com.bugsnag.android.internal.JsonHelper
import java.io.IOException
import java.net.HttpURLConnection
import java.net.HttpURLConnection.HTTP_ENTITY_TOO_LARGE
import java.net.URL

internal class DefaultDelivery(
    private val connectivity: Connectivity?,
    private val logger: Logger
) : Delivery {

    override fun deliver(payload: Session, deliveryParams: DeliveryParams): DeliveryStatus {
        val status = deliver(
            deliveryParams.endpoint,
            JsonHelper.serialize(payload),
            payload.integrityToken,
            deliveryParams.headers
        )
        logger.i("Session API request finished with status $status")
        return status
    }

    override fun deliver(payload: EventPayload, deliveryParams: DeliveryParams): DeliveryStatus {
        val json = payload.toByteArray()
        val status = deliverImpl(deliveryParams.endpoint, json, payload.integrityToken, deliveryParams.headers)
        if (status == HTTP_ENTITY_TOO_LARGE) {
            val newJson = payload.trimToSize().toByteArray()
            deliverImpl(deliveryParams.endpoint, newJson, payload.integrityToken, deliveryParams.headers)
        }
        logger.i("Error API request finished with status $status")
        return DeliveryStatus.forHttpResponseCode(status)
    }

    private fun deliverImpl(
        urlString: String,
        json: ByteArray,
        integrity: String?,
        headers: Map<String, String?>
    ): Int {

        TrafficStats.setThreadStatsTag(1)
        if (connectivity != null && !connectivity.hasNetworkConnection()) {
            return UNDELIVERED
        }
        var conn: HttpURLConnection? = null
        try {
            conn = makeRequest(URL(urlString), json, integrity, headers)

            // End the request, get the response code
            val responseCode = conn.responseCode
            logRequestInfo(responseCode, conn)
            return responseCode
        } catch (oom: OutOfMemoryError) {
            // attempt to persist the payload on disk. This approach uses streams to write to a
            // file, which takes less memory than serializing the payload into a ByteArray, and
            // therefore has a reasonable chance of retaining the payload for future delivery.
            logger.w("Encountered OOM delivering payload, falling back to persist on disk", oom)
            return UNDELIVERED
        } catch (exception: IOException) {
            logger.w("IOException encountered in request", exception)
            return UNDELIVERED
        } catch (exception: Exception) {
            logger.w("Unexpected error delivering payload", exception)
            return FAILURE
        } finally {
            conn?.disconnect()
        }
    }

    fun deliver(
        urlString: String,
        json: ByteArray,
        integrity: String?,
        headers: Map<String, String?>
    ): DeliveryStatus {
        return DeliveryStatus.forHttpResponseCode(deliverImpl(urlString, json, integrity, headers))
    }

    private fun makeRequest(
        url: URL,
        json: ByteArray,
        integrity: String?,
        headers: Map<String, String?>
    ): HttpURLConnection {
        val conn = url.openConnection() as HttpURLConnection
        conn.doOutput = true

        // avoids creating a buffer within HttpUrlConnection, see
        // https://developer.android.com/reference/java/net/HttpURLConnection
        conn.setFixedLengthStreamingMode(json.size)

        integrity?.let { digest ->
            conn.addRequestProperty(HEADER_BUGSNAG_INTEGRITY, digest)
        }
        headers.forEach { (key, value) ->
            if (value != null) {
                conn.addRequestProperty(key, value)
            }
        }

        // write the JSON payload
        conn.outputStream.use {
            it.write(json)
        }
        return conn
    }

    private fun logRequestInfo(code: Int, conn: HttpURLConnection) {
        runCatching {
            logger.i(
                "Request completed with code $code, " +
                    "message: ${conn.responseMessage}, " +
                    "headers: ${conn.headerFields}"
            )
        }
        runCatching {
            conn.inputStream.bufferedReader().use {
                logger.d("Received request response: ${it.readText()}")
            }
        }

        runCatching {
            if (code != DELIVERED) {
                conn.errorStream.bufferedReader().use {
                    logger.w("Request error details: ${it.readText()}")
                }
            }
        }
    }

    companion object {
        const val DELIVERED = -1
        const val UNDELIVERED = -2
        const val FAILURE = -3
    }
}
