package com.bugsnag.android.mazerunner.scenarios

import com.bugsnag.android.Bugsnag

/**
 * Sends a handled exception to Bugsnag, which does not include session data.
 */
internal class HandledExceptionScenario : Scenario() {

    override fun run() {
        Bugsnag.notify(RuntimeException("HandledExceptionScenario"))
    }

}
