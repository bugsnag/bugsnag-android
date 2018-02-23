package com.bugsnag.android.mazerunner.scenarios

import com.bugsnag.android.Bugsnag

/**
 * Attempts to deliver a handled exception with no stacktrace.
 */
internal class EmptyStacktraceScenario : Scenario() {

    override fun run() {
        Bugsnag.notify(EmptyException("EmptyStacktraceScenario"))
    }

    class EmptyException(message: String?) : Throwable(message, null, true, false)

}
