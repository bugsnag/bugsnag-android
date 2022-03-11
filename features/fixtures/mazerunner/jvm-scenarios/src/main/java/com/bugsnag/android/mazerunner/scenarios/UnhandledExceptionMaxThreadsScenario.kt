package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.Configuration

/**
 * Sends an unhandled exception to Bugsnag.
 */
internal class UnhandledExceptionMaxThreadsScenario(
    config: Configuration,
    context: Context,
    eventMetadata: String
) : Scenario(config, context, eventMetadata) {

    init {
        config.maxReportedThreads = 2
    }

    override fun startScenario() {
        super.startScenario()
        throw generateException()
    }
}
