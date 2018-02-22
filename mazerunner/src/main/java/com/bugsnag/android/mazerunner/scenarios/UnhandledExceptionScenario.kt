package com.bugsnag.android.mazerunner.scenarios

/**
 * Sends an unhandled exception to Bugsnag.
 */
internal class UnhandledExceptionScenario : Scenario() {

    override fun run() {
        throw generateException()
    }

}
