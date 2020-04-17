package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.Configuration

/**
 * Attempts to send an ignored handled exception to Bugsnag, which should not result
 * in any operation.
 */
internal class IgnoredExceptionScenario(config: Configuration,
                                        context: Context) : Scenario(config, context) {
    init {
        config.autoTrackSessions = false
        config.discardClasses = setOf("java.lang.RuntimeException")
    }

    override fun run() {
        super.run()
        throw RuntimeException("Should never appear")
    }

}
