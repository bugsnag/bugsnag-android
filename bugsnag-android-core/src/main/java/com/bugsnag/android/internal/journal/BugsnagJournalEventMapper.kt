package com.bugsnag.android.internal.journal

import com.bugsnag.android.BugsnagJournal
import com.bugsnag.android.EventInternal
import com.bugsnag.android.Logger
import com.bugsnag.android.User
import java.io.File

/**
 * Converts a Bugsnag journal entry into an Event.
 */
internal class BugsnagJournalEventMapper(
    private val logger: Logger
) {

    fun convertToEvent(baseDocumentPath: File): EventInternal? {
        return try {
            when (val map = BugsnagJournal.loadPreviousDocument(baseDocumentPath)) {
                null -> null
                else -> convertToEvent(map)
            }
        } catch (exc: Throwable) {
            logger.e("Failed to load journal, skipping event", exc)
            null
        }
    }

    fun convertToEvent(map: Map<in String, Any?>): EventInternal? {
        return try {
            convertToEventImpl(map)
        } catch (exc: Throwable) {
            logger.e("Failed to deserialize journal, skipping event", exc)
            null
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun convertToEventImpl(map: Map<in String, Any?>): EventInternal {
        logger.d("Read previous journal, contents=$map")
        val apiKey: String = map.readJournalEntry("apiKey")
        val event = EventInternal(apiKey)

        // populate user
        val userMap: Map<String, String?> = map.readJournalEntry("user")
        event.userImpl = User(userMap)

        // populate metadata
        val metadataMap: Map<String, Map<String, Any?>> = map.readJournalEntry("metaData")
        metadataMap.forEach { (key, value) ->
            event.addMetadata(key, value)
        }
        return event
    }

    /**
     * Convenience method for getting an entry from a Map in the expected type, which
     * throws useful error messages if the expected type is not there.
     */
    private inline fun <reified T> Map<*, *>.readJournalEntry(key: String): T {
        check(containsKey(key)) {
            "Journal does not contain entry for '$key'"
        }
        val value = requireNotNull(get(key))
        check(value is T) {
            "Journal entry for '$key' not of expected type, found ${value.javaClass.name}"
        }
        return value
    }
}
