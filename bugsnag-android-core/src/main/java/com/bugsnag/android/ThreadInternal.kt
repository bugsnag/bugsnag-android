package com.bugsnag.android

import com.bugsnag.android.internal.JournalKeys
import com.bugsnag.android.internal.Journalable
import java.io.IOException

class ThreadInternal internal constructor(
    var id: Long,
    var name: String,
    var type: ThreadType,
    val isErrorReportingThread: Boolean,
    stacktrace: Stacktrace
) : JsonStream.Streamable, Journalable {

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

    override fun toJournalSection(): Map<String, Any?> {
        return mapOf(
            JournalKeys.keyId to id,
            JournalKeys.keyName to name,
            JournalKeys.keyType to type.desc,
            JournalKeys.keyErrorReportingThread to isErrorReportingThread,
            JournalKeys.keyStackTrace to stacktrace.map { it.toJournalSection() }
        )
    }
}
