package com.bugsnag.android.mazerunner.scenarios

import android.annotation.SuppressLint
import android.content.Context
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration

@SuppressLint("NewApi")
class MyThrowable(message: String?) : Throwable(message, null, false, false) {
    override fun getStackTrace() = null
}

/**
 * Sends an unhandled exception to Bugsnag.
 */
internal class NullStackTraceScenario(
    config: Configuration,
    context: Context,
    eventMetadata: String
) : Scenario(config, context, eventMetadata) {

    override fun startScenario() {
        super.startScenario()
        Bugsnag.notify(MyThrowable("NullStackTraceScenario"))
    }
}
