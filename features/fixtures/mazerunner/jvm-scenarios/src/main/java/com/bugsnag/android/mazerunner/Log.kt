package com.bugsnag.android.mazerunner

import android.util.Log

fun log(msg: String) {
    Log.d("BugsnagMazeRunner", msg)
}

/**
 * Gets the log messages expected when zero events should be sent to Bugsnag.
 */
fun getZeroEventsLogMessages(eventMetadata: String?): List<String> {
    return listOf(
        "No startupcrash events to flush to Bugsnag.",
        "No regular events to flush to Bugsnag."
    )
}
