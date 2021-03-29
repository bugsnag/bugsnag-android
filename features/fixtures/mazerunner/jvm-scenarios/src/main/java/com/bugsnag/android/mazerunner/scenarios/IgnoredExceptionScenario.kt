package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration

/**
 * Attempts to send an ignored handled exception to Bugsnag, which should not result
 * in any operation.
 */
internal class IgnoredExceptionScenario(
    config: Configuration,
    context: Context,
    eventMetadata: String
) : Scenario(config, context, eventMetadata) {

    init {
        config.discardClasses = setOf("java.lang.RuntimeException")
    }

    override fun startScenario() {
        super.startScenario()
        Bugsnag.notify(RuntimeException("Should never appear"))
    }

    override fun getInterceptedLogMessages() =
        listOf("Skipping notification - should not notify for this class")
}
