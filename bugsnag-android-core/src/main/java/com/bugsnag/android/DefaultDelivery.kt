package com.bugsnag.android

import android.net.TrafficStats
import com.bugsnag.android.internal.HEADER_BUGSNAG_INTEGRITY
import com.bugsnag.android.internal.JsonHelper
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.GZIPOutputStream

internal class DefaultDelivery(
    private val connectivity: Connectivity?,
    private val logger: Logger
) : Delivery {

    override fun deliver(payload: Session, deliveryParams: DeliveryParams): DeliveryStatus {
        val status = deliver(
            deliveryParams.endpoint,
            JsonHelper.serialize(payload),
            payload.integrityToken,
            deliveryParams.headers,
            deliveryParams.payloadEncoding
        )

        logger.i("Session API request finished with status $status")
        return status
    }

    override fun deliver(payload: EventPayload, deliveryParams: DeliveryParams): DeliveryStatus {
        val json = payload.trimToSize().toByteArray()
        val status = deliver(
            deliveryParams.endpoint,
            json,
            payload.integrityToken,
            deliveryParams.headers,
            deliveryParams.payloadEncoding
        )

        logger.i("Error API request finished with status $status")
        return status
    }

    fun deliver(
        urlString: String,
        json: ByteArray,
        integrity: String?,
        headers: Map<String, String?>,
        encoding: DeliveryParams.PayloadEncoding = DeliveryParams.PayloadEncoding.NONE
    ): DeliveryStatus {

        TrafficStats.setThreadStatsTag(1)
        if (connectivity != null && !connectivity.hasNetworkConnection()) {
            return DeliveryStatus.UNDELIVERED
        }
        var conn: HttpURLConnection? = null

        val status = try {
            conn = makeRequest(URL(urlString), json, integrity, headers, encoding)

            // End the request, get the response code
            val responseCode = conn.responseCode
            val status = DeliveryStatus.forHttpResponseCode(responseCode)
            logRequestInfo(responseCode, conn, status)
            status
        } catch (oom: OutOfMemoryError) {
            // attempt to persist the payload on disk. This approach uses streams to write to a
            // file, which takes less memory than serializing the payload into a ByteArray, and
            // therefore has a reasonable chance of retaining the payload for future delivery.
            logger.w("Encountered OOM delivering payload, falling back to persist on disk", oom)
            DeliveryStatus.UNDELIVERED
        } catch (exception: IOException) {
            logger.w("IOException encountered in request", exception)
            DeliveryStatus.UNDELIVERED
        } catch (exception: Exception) {
            logger.w("Unexpected error delivering payload", exception)
            DeliveryStatus.FAILURE
        } finally {
            conn?.disconnect()
        }

        return status
    }

    private fun makeRequest(
        url: URL,
        json: ByteArray,
        integrity: String?,
        headers: Map<String, String?>,
        encoding: DeliveryParams.PayloadEncoding
    ): HttpURLConnection {
        val conn = url.openConnection() as HttpURLConnection
        conn.doOutput = true

        integrity?.let { digest ->
            conn.addRequestProperty(HEADER_BUGSNAG_INTEGRITY, digest)
        }
        headers.forEach { (key, value) ->
            if (value != null) {
                conn.addRequestProperty(key, value)
            }
        }

        if (encoding == DeliveryParams.PayloadEncoding.GZIP) {
            // we don't use fixedLengthStreamingMode to avoid making yet-another gzipped copy

            conn.addRequestProperty("Content-Encoding", "gzip")
            GZIPOutputStream(conn.outputStream).use {
                it.write(json)
            }
        } else {
            // avoids creating a buffer within HttpUrlConnection, see
            // https://developer.android.com/reference/java/net/HttpURLConnection
            conn.setFixedLengthStreamingMode(json.size)

            // write the JSON payload
            conn.outputStream.use {
                it.write(json)
            }
        }
        return conn
    }

    private fun logRequestInfo(code: Int, conn: HttpURLConnection, status: DeliveryStatus) {
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
            if (status != DeliveryStatus.DELIVERED) {
                conn.errorStream.bufferedReader().use {
                    logger.w("Request error details: ${it.readText()}")
                }
            }
        }
    }
}
