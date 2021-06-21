package com.bugsnag.android.anrapp

import android.util.Log

const val LOG_TAG = "AnrApplication"

inline fun withLogMessage(message: String, block: () -> Unit) {
    Log.i(LOG_TAG, message)
    block()
    Log.i(LOG_TAG, "$message - done")
}