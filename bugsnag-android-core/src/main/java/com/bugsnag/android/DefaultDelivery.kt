package com.bugsnag.android

import java.io.IOException
import java.net.HttpURLConnection
import java.net.HttpURLConnection.HTTP_BAD_REQUEST
import java.net.HttpURLConnection.HTTP_CLIENT_TIMEOUT
import java.net.HttpURLConnection.HTTP_OK
import java.net.URL

internal class DefaultDelivery(private val connectivity: Connectivity?, val logger: Logger) : Delivery {

    override fun deliver(payload: Session, deliveryParams: DeliveryParams): DeliveryStatus {
        val status = deliver(deliveryParams.endpoint, payload, deliveryParams.headers)
        logger.i("Session API request finished with status $status")
        return status
    }

    override fun deliver(payload: EventPayload, deliveryParams: DeliveryParams): DeliveryStatus {
        val status = deliver(deliveryParams.endpoint, payload, deliveryParams.headers)
        logger.i("Error API request finished with status $status")
        return status
    }

    fun deliver(
        urlString: String,
        streamable: JsonStream.Streamable,
        headers: Map<String, String?>
    ): DeliveryStatus {

        if (connectivity != null && !connectivity.hasNetworkConnection()) {
            return DeliveryStatus.UNDELIVERED
        }
        var conn: HttpURLConnection? = null

        try {
            val url = URL(urlString)
            conn = url.openConnection() as HttpURLConnection
            conn.doOutput = true
            conn.setChunkedStreamingMode(0)
            conn.addRequestProperty("Content-Type", "application/json")

            headers.forEach { (key, value) ->
                if (value != null) {
                    conn.addRequestProperty(key, value)
                }
            }

            conn.outputStream.bufferedWriter().use { writer ->
                streamable.toStream(JsonStream(writer))
            }

            // End the request, get the response code
            val responseCode = conn.responseCode
            val status = getDeliveryStatus(responseCode)
            logRequestInfo(responseCode, conn, status)
            return status
        } catch (exception: IOException) {
            logger.w("IOException encountered in request", exception)
            return DeliveryStatus.UNDELIVERED
        } catch (exception: Exception) {
            logger.w("Unexpected error delivering payload", exception)
            return DeliveryStatus.FAILURE
        } finally {
            IOUtils.close(conn)
        }
    }

    private fun logRequestInfo(code: Int, conn: HttpURLConnection, status: DeliveryStatus) {
        logger.i(
            "Request completed with code $code, " +
                    "message: ${conn.responseMessage}, " +
                    "headers: ${conn.headerFields}"
        )

        if (status != DeliveryStatus.DELIVERED) {
            val errBody = conn.errorStream.bufferedReader().readText()
            logger.w("Request error details: $errBody")
        }
    }

    internal fun getDeliveryStatus(responseCode: Int): DeliveryStatus {
        val unrecoverableCodes = IntRange(HTTP_BAD_REQUEST, 499).filter {
            it != HTTP_CLIENT_TIMEOUT && it != 429
        }

        return when (responseCode) {
            in HTTP_OK..299 -> DeliveryStatus.DELIVERED
            in unrecoverableCodes -> DeliveryStatus.FAILURE
            else -> DeliveryStatus.UNDELIVERED
        }
    }

}
