package com.bugsnag.android

import com.bugsnag.android.internal.journal.JournalKeys
import com.bugsnag.android.internal.journal.Journalable
import java.io.IOException

/**
 * Information about this library, including name and version.
 */
class Notifier @JvmOverloads constructor(
    var name: String = "Android Bugsnag Notifier",
    var version: String = "5.15.0",
    var url: String = "https://bugsnag.com"
) : JsonStream.Streamable, Journalable {

    var dependencies = listOf<Notifier>()

    @Throws(IOException::class)
    override fun toStream(writer: JsonStream) = writer.value(toJournalSection())

    override fun toJournalSection(): Map<String, Any?> {
        val data = mutableMapOf(
            JournalKeys.keyName to name,
            JournalKeys.keyVersion to version,
            JournalKeys.keyUrl to url
        )
        return when {
            dependencies.isNotEmpty() -> data.plus(
                Pair("dependencies", dependencies.map(Notifier::toJournalSection))
            )
            else -> data
        }
    }
}
