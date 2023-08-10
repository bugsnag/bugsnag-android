package com.bugsnag.android.mazerunner

import android.util.Log

fun log(msg: String) {
    Log.d("BugsnagMazeRunner", msg)
}

fun log(msg: String, e: Exception) {
    Log.e("BugsnagMazeRunner", msg, e)
}

object CiLog {
    fun info(msg: String) = Log.i("bugsnagci info", msg)
    fun warn(msg: String) = Log.w("bugsnagci warn", msg)
    fun error(msg: String) = Log.e("bugsnagci error", msg)
    fun error(msg: String, e: Exception) = Log.e("bugsnagci error", msg, e)
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
