package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration

/**
 * Attempts to send an ignored handled exception to Bugsnag, which should not result
 * in any operation.
 */
internal class IgnoredExceptionScenario(config: Configuration,
                                        context: Context) : Scenario(config, context) {

    override fun run() {
        super.run()
        Bugsnag.setIgnoreClasses("java.lang.RuntimeException")
        throw RuntimeException("Should never appear")
    }

}
