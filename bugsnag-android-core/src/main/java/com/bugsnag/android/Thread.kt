package com.bugsnag.android

import java.io.IOException

/**
 * A representation of a thread recorded in a [Report]
 */
class Thread internal constructor(
    val id: Long,
    val name: String,
    val type: Type,
    val isErrorReportingThread: Boolean,
    stacktrace: Stacktrace
) : JsonStream.Streamable {

    enum class Type(internal val desc: String) {
        ANDROID("android"),
        BROWSER_JS("browserjs")
    }

    enum class ThreadSendPolicy {
        ALWAYS,
        UNHANDLED_ONLY,
        NEVER
    }

    var stacktrace: MutableList<Stackframe> = stacktrace.trace.toMutableList()

    @Throws(IOException::class)
    override fun toStream(writer: JsonStream) {
        writer.beginObject()
        writer.name("id").value(id)
        writer.name("name").value(name)
        writer.name("type").value(type.desc)

        writer.name("stacktrace")
        writer.beginArray()
        stacktrace.forEach { writer.value(it) }
        writer.endArray()

        if (isErrorReportingThread) {
            writer.name("errorReportingThread").value(true)
        }
        writer.endObject()
    }
}
