package com.bugsnag.android

import java.io.IOException

/**
 * A representation of a thread recorded in a [Report]
 */
class Thread internal constructor(
    val id: Long,
    val name: String,
    val type: String,
    val isErrorReportingThread: Boolean,
    private val stacktrace: Stacktrace
) : JsonStream.Streamable {

    @Throws(IOException::class)
    override fun toStream(writer: JsonStream) {
        writer.beginObject()
        writer.name("id").value(id)
        writer.name("name").value(name)
        writer.name("type").value(type)
        writer.name("stacktrace").value(stacktrace)
        if (isErrorReportingThread) {
            writer.name("errorReportingThread").value(true)
        }
        writer.endObject()
    }
}
