package com.bugsnag.android.mazerunner.scenarios

import android.annotation.SuppressLint
import android.content.Context
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration

/**
 * Attempts to deliver a handled exception with no stacktrace.
 */
internal class EmptyStacktraceScenario(config: Configuration,
                                       context: Context) : Scenario(config, context) {
    init {
        config.autoTrackSessions = false
    }

    override fun run() {
        super.run()
        Bugsnag.notify(EmptyException("EmptyStacktraceScenario"))
    }

    @SuppressLint("NewApi")
    class EmptyException(message: String?) : Throwable(message, null, true, false)

}
