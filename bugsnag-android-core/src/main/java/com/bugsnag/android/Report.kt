package com.bugsnag.android

import java.io.File
import java.io.IOException

/**
 * An error report payload.
 *
 * This payload contains an error report and identifies the source application
 * using your API key.
 */
class Report : JsonStream.Streamable {

    var apiKey: String?
    private val eventFile: File?
    val event: Event?

    internal constructor(apiKey: String?, eventFile: File) {
        this.apiKey = apiKey
        this.eventFile = eventFile
        this.event = null
    }

    internal constructor(apiKey: String?, event: Event) {
        this.apiKey = apiKey
        this.eventFile = null
        this.event = event
    }

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
            else -> Unit
        }

        writer.endArray()
        writer.endObject()
    }
}
