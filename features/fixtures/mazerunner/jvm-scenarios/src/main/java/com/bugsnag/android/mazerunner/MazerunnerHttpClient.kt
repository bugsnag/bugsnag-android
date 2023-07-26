package com.bugsnag.android.mazerunner

import android.util.JsonWriter
import java.io.StringWriter
import java.net.HttpURLConnection
import java.net.URL
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

        JsonWriter(connection.outputStream.bufferedWriter()).use { json ->
            json.beginObject()

            values.forEach { (name, value) ->
                json.name(name).value(value)
            }

            json.endObject()
        }

        // Make sure that we wait for the Mazerunner server to respond
        val responseCode = connection.responseCode
        log("${values.size} values delivered as metrics, response=$responseCode")

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
