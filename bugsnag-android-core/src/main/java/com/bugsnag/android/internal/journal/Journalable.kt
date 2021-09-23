package com.bugsnag.android.internal.journal

interface Journalable {

    /**
     * Creates a section that can be serialized as an entry in the journal.
     */
    fun toJournalSection(): Map<String, Any?>
}
