package com.bugsnag.android.internal.journal

import com.bugsnag.android.BugsnagJournal
import com.bugsnag.android.Event
import com.bugsnag.android.Logger
import java.io.File

/**
 * Converts a Bugsnag journal entry into an Event.
 */
internal class BugsnagJournalEventMapper(
    private val logger: Logger
) {

    fun convertToEvent(baseDocumentPath: File): Event? {
        return try {
            val map = BugsnagJournal.loadPreviousDocument(baseDocumentPath)
            convertToEvent(map)
        } catch (exc: Throwable) {
            logger.e("Failed to load journal, skipping event", exc)
            null
        }
    }

    fun convertToEvent(map: Map<in String, Any>?): Event? {
        logger.d("Read previous journal, contents=$map")
        return null
    }
}
