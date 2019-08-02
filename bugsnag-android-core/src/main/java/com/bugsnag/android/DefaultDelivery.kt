package com.bugsnag.android

import java.io.BufferedWriter
import java.io.IOException
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.Charset

internal class DefaultDelivery(private val connectivity: Connectivity?) : Delivery {

    override fun deliver(payload: SessionTrackingPayload,
                         deliveryParams: DeliveryParams): DeliveryStatus {
        val status = deliver(deliveryParams.endpoint, payload, deliveryParams.headers)
        Logger.info("Session API request finished with status $status")
        return status
    }

    override fun deliver(report: Report, deliveryParams: DeliveryParams): DeliveryStatus {
        val status = deliver(deliveryParams.endpoint, report, deliveryParams.headers)
        Logger.info("Error API request finished with status $status")
        return status
    }

    fun deliver(
        urlString: String,
        streamable: JsonStream.Streamable,
        headers: Map<String, String>
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
                conn.addRequestProperty(key, value)
            }

            var stream: JsonStream? = null

            try {
                val out = conn.outputStream
                val charset = Charset.forName("UTF-8")
                val writer = BufferedWriter(OutputStreamWriter(out, charset))
                stream = JsonStream(writer)
                streamable.toStream(stream)
            } finally {
                IOUtils.closeQuietly(stream)
            }

            // End the request, get the response code
            return getDeliveryStatus(conn.responseCode)
        } catch (exception: IOException) {
            Logger.warn("IOException encountered in request", exception)
            return DeliveryStatus.UNDELIVERED
        } catch (exception: Exception) {
            Logger.warn("Unexpected error delivering payload", exception)
            return DeliveryStatus.FAILURE
        } finally {
            IOUtils.close(conn)
        }
    }

    internal fun getDeliveryStatus(responseCode: Int): DeliveryStatus {
        return when (responseCode) {
            in 200..299 -> DeliveryStatus.DELIVERED
            in 400..499 -> DeliveryStatus.FAILURE
            else -> DeliveryStatus.UNDELIVERED
        }
    }

}
