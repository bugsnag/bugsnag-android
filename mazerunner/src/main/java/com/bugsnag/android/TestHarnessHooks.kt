package com.bugsnag.android

/**
 * Accesses the session tracker and flushes all stored sessions
 */
internal fun flushAllSessions() {
    Bugsnag.getClient().sessionTracker.flushStoredSessions()
}
