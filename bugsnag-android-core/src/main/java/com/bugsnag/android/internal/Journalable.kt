package com.bugsnag.android.internal

interface Journalable {
    fun toJournalSection(): Map<String, Any?>
}
