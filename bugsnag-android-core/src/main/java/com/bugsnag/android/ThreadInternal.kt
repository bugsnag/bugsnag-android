package com.bugsnag.android

import com.bugsnag.android.internal.journal.JournalKeys
import com.bugsnag.android.internal.journal.Journalable
import java.io.IOException

class ThreadInternal internal constructor(
    var id: Long,
    var name: String,
    var type: ThreadType,
    val isErrorReportingThread: Boolean,
    var state: String,
    stacktrace: Stacktrace
) : JsonStream.Streamable, Journalable {

    var stacktrace: MutableList<Stackframe> = stacktrace.trace.toMutableList()

    @Throws(IOException::class)
    override fun toStream(writer: JsonStream) = writer.value(toJournalSection())

    override fun toJournalSection(): Map<String, Any?> {
        val data = mutableMapOf(
            JournalKeys.keyId to id,
            JournalKeys.keyName to name,
            JournalKeys.keyType to type.desc,
            JournalKeys.keyState to state
        )

        if (stacktrace.isNotEmpty()) {
            data[JournalKeys.keyStackTrace] = stacktrace.map { it.toJournalSection() }
        }
        if (isErrorReportingThread) {
            data[JournalKeys.keyErrorReportingThread] = isErrorReportingThread
        }

        return data
    }
}
