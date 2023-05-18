package com.bugsnag.android.mazerunner

import android.util.JsonWriter
import java.io.ByteArrayOutputStream
import java.io.StringWriter
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

enum class LogLevel {
    ERROR,
    WARNING,
    INFO,
    DEBUG
}

class MazerunnerHttpClient(
    private val logEndpoint: URL,
    private val metricsEndpoint: URL
) {

    private val executor = Executors.newSingleThreadExecutor()

    fun postLog(logLevel: LogLevel, msg: String) {
        executor.submit {
            val level = logLevel.toString().toLowerCase(Locale.US)
            val json = generateJson(level, msg)

            // make a HTTP request
            val conn = logEndpoint.openConnection() as HttpURLConnection
            conn.doOutput = true

            // write the JSON payload
            conn.outputStream.bufferedWriter().use {
                it.write(json)
            }
            log("Mazerunner log request completed with status ${conn.responseCode}")
            conn.disconnect()
        }
    }

    fun postMetric(vararg values: Pair<String, String>) = executor.executeAwait {
        val connection = metricsEndpoint.openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.setRequestProperty("Content-Type", "application/json")
        connection.doOutput = true

        val body = ByteArrayOutputStream().use { bytes ->
            JsonWriter(bytes.writer()).use { json ->
                json.beginObject()

                values.forEach { (name, value) ->
                    json.name(name).value(value)
                }

                json.endObject()
            }

            bytes.toByteArray()
        }

        // Mazerunner expects all of these requests to also have a Bugsnag-Integrity header
        connection.setRequestProperty("Bugsnag-Integrity", "sha1 ${body.sha1()}")
        connection.outputStream.write(body)

        connection.disconnect()
    }

    private fun generateJson(level: String, msg: String): String {
        val stringWriter = StringWriter()
        JsonWriter(stringWriter).use { writer ->
            writer.beginObject()
            writer.name("level").value(level)
            writer.name("message").value(msg)
            writer.endObject()
        }
        return stringWriter.toString()
    }

    private fun ExecutorService.executeAwait(block: () -> Unit) {
        submit(block).get()
    }

    @Suppress("MagicNumber") // all hex-encoding-related values
    private fun ByteArray.sha1(): String {
        val digester = MessageDigest.getInstance("SHA-1")
        val digest = digester.digest(this)

        return buildString(digest.size * 2) {
            digest.forEach { b ->
                val byte = b.toInt() and 0xff
                if (byte < 16) append('0')
                append(byte.toString(16))
            }
        }
    }

    override fun toString(): String {
        return "MazerunnerHttpClient(logEndpoint=$logEndpoint, metricsEndpoint=$metricsEndpoint)"
    }

    companion object {
        fun fromEndpoint(endpoint: URL): MazerunnerHttpClient {
            return MazerunnerHttpClient(
                URL(endpoint.protocol, endpoint.host, endpoint.port, "/logs"),
                URL(endpoint.protocol, endpoint.host, endpoint.port, "/metrics")
            )
        }

        fun fromEndpoint(endpointUrl: String): MazerunnerHttpClient {
            return fromEndpoint(URL(endpointUrl))
        }
    }
}
