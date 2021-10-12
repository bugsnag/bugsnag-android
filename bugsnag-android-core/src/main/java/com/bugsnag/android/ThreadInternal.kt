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
        val data = mapOf(
            JournalKeys.keyId to id,
            JournalKeys.keyName to name,
            JournalKeys.keyType to type.desc,
            JournalKeys.keyState to state,
            JournalKeys.keyStackTrace to stacktrace.map { it.toJournalSection() }
        )

        return when {
            isErrorReportingThread -> data.plus(
                Pair(JournalKeys.keyErrorReportingThread, isErrorReportingThread)
            )
            else -> data
        }
    }
}
