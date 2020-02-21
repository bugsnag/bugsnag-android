package com.bugsnag.android

import java.io.IOException

/**
 * A representation of a thread recorded in an [Event]
 */
class ThreadImpl internal constructor(

    /**
     * The unique ID of the thread (from [java.lang.Thread])
     */
    var id: Long,

    /**
     * The name of the thread (from [java.lang.Thread])
     */
    var name: String,

    /**
     * The type of thread based on the originating platform (intended for internal use only)
     */
    var type: ThreadType,

    /**
     * Whether the thread was the thread that caused the event
     */
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
