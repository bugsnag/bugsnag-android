package com.bugsnag.android

import java.io.IOException

class ThreadImpl internal constructor(
    var id: Long,
    var name: String,
    var type: ThreadType,
    var isErrorReportingThread: Boolean,
    stacktrace: Stacktrace
) : JsonStream.Streamable {

    /**
     * A representation of the thread's stacktrace
     */
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
