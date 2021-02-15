package com.bugsnag.android.mazerunner

import android.util.JsonWriter
import java.io.StringWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import java.util.concurrent.Executors

enum class LogLevel {
    ERROR,
    WARNING,
    INFO,
    DEBUG
}

class MazerunnerHttpClient(private val logEndpoint: URL) {

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
}
