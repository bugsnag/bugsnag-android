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

    fun getErrorTypes(): String? {
        return when {
            event != null -> getErrorTypesFromStackframes(event)
            eventFile != null -> getErrorTypesFromFilename(eventFile)
            else -> null
        }
    }

    private fun getErrorTypesFromStackframes(event: Event): String? {
        val errorTypes = event.errors.mapNotNull(Error::getType).toSet()
        val frameOverrideTypes = event.errors
            .map { it.stacktrace }
            .flatMap { it.mapNotNull(Stackframe::type) }

        val distinctTypes = errorTypes.plus(frameOverrideTypes)
        return serializeErrorTypeHeader(distinctTypes)
    }

    private fun getErrorTypesFromFilename(eventFile: File): String? {
        val name = eventFile.name
        val end = name.lastIndexOf("_", name.lastIndexOf("_") - 1)
        val start = name.lastIndexOf("_", end - 1) + 1

        if (start < end) {
            val errorTypes = name.substring(start, end)
            val validValues = ErrorType.values().map { it.desc }

            // validate that this only contains valid error type info
            val valid = errorTypes.split(",").all { errorType ->
                validValues.contains(errorType)
            }
            if (valid) {
                return errorTypes
            }
        }
        return null
    }

    /**
     * Serializes the error types to a comma delimited string
     */
    private fun serializeErrorTypeHeader(errorTypes: Set<ErrorType>): String? {
        return when {
            errorTypes.isEmpty() -> null
            else -> errorTypes
                .map(ErrorType::desc)
                .reduce { accumulator, str ->
                    "$accumulator,$str"
                }
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
