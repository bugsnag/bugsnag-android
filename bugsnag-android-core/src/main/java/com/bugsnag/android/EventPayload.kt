package com.bugsnag.android

import java.io.File
import java.io.IOException

/**
 * An error report payload.
 *
 * This payload contains an error report and identifies the source application
 * using your API key.
 */
class EventPayload : JsonStream.Streamable {

    var apiKey: String?
    private val eventFile: File?
    val event: Event?
    private val notifier: Notifier
    private val config: ImmutableConfig

    internal constructor(
        apiKey: String?,
        eventFile: File,
        notifier: Notifier,
        config: ImmutableConfig
    ) {
        this.apiKey = apiKey
        this.eventFile = eventFile
        this.event = null
        this.notifier = notifier
        this.config = config
    }

    internal constructor(
        apiKey: String?,
        event: Event,
        notifier: Notifier,
        config: ImmutableConfig
    ) {
        this.apiKey = apiKey
        this.eventFile = null
        this.event = event
        this.notifier = notifier
        this.config = config
    }

    internal fun getErrorTypes(): Set<ErrorType> {
        return when {
            event != null -> event.impl.getErrorTypesFromStackframes()
            eventFile != null -> EventFilenameInfo.fromFile(eventFile, config).errorTypes
            else -> emptySet()
        }
    }

    @Throws(IOException::class)
    override fun toStream(writer: JsonStream) {
        writer.beginObject()
        writer.name("apiKey").value(apiKey)
        writer.name("payloadVersion").value("4.0")
        writer.name("notifier").value(notifier)
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
