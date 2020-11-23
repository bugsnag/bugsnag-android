package com.bugsnag.android

import com.bugsnag.android.EventFilenameInfo.Companion.findErrorTypesInFilename
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

    internal constructor(apiKey: String?, eventFile: File, notifier: Notifier) {
        this.apiKey = apiKey
        this.eventFile = eventFile
        this.event = null
        this.notifier = notifier
    }

    internal constructor(apiKey: String?, event: Event, notifier: Notifier) {
        this.apiKey = apiKey
        this.eventFile = null
        this.event = event
        this.notifier = notifier
    }

    internal fun getErrorTypes(): Set<ErrorType> {
        return when {
            event != null -> getErrorTypesFromStackframes(event)
            eventFile != null -> findErrorTypesInFilename(eventFile)
            else -> emptySet()
        }
    }

    private fun getErrorTypesFromStackframes(event: Event): Set<ErrorType> {
        val errorTypes = event.errors.mapNotNull(Error::getType).toSet()
        val frameOverrideTypes = event.errors
            .map { it.stacktrace }
            .flatMap { it.mapNotNull(Stackframe::type) }
        return errorTypes.plus(frameOverrideTypes)
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
