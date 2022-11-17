package com.bugsnag.android.mazerunner

import android.os.SystemClock
import android.util.Log

fun log(msg: String) {
    Log.d("BugsnagMazeRunner", msg)
}

fun log(msg: String, e: Exception) {
    Log.e("BugsnagMazeRunner", msg, e)
}

inline fun <R> reportDuration(tag: String, block: () -> R): R {
    val start = SystemClock.elapsedRealtime()
    try {
        return block()
    } finally {
        val end = SystemClock.elapsedRealtime()
        Log.i("MazeDuration", "$tag - ${end - start}ms")
    }
}

/**
 * Gets the log messages expected when zero events should be sent to Bugsnag.
 */
fun getZeroEventsLogMessages(startBugsnagOnly: Boolean): List<String> {
    return if (startBugsnagOnly) {
        listOf(
            "No startupcrash events to flush to Bugsnag.",
            "No regular events to flush to Bugsnag."
        )
    } else {
        emptyList()
    }
}
