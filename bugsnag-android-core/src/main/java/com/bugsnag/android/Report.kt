package com.bugsnag.android

import java.io.File
import java.io.IOException

/**
 * An error report payload.
 *
 *
 * This payload contains an error report and identifies the source application
 * using your API key.
 */
class Report internal constructor(
    var apiKey: String?,
    private val eventFile: File? = null,
    val event: Event? = null
) : JsonStream.Streamable {

    @Throws(IOException::class)
    override fun toStream(writer: JsonStream) {
        writer.beginObject()
        writer.name("apiKey").value(apiKey)
        writer.name("payloadVersion").value("4.0")
        writer.name("notifier").value(Notifier)

        writer.name("events").beginArray()

        when {
            event != null -> writer.value(event)
            eventFile != null -> writer.value(eventFile)
            else -> Logger.warn("Expected event or eventFile, found empty payload instead")
        }

        writer.endArray()
        writer.endObject()
    }
}
