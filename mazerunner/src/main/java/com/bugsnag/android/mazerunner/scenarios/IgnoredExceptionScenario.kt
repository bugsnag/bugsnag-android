package com.bugsnag.android.mazerunner.scenarios

import com.bugsnag.android.Bugsnag

/**
 * Attempts to send an ignored handled exception to Bugsnag, which should not result
 * in any operation.
 */
internal class IgnoredExceptionScenario : Scenario() {

    override fun run() {
        Bugsnag.setIgnoreClasses("java.lang.RuntimeException")
        throw RuntimeException("Should never appear")
    }

}
