package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration
import java.lang.IllegalStateException
import java.util.regex.Pattern

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
        config.discardClasses = setOf(Pattern.compile(".*java.lang.RuntimeException.*"))
    }

    override fun startScenario() {
        super.startScenario()
        Bugsnag.notify(RuntimeException("Should never appear"))
        Bugsnag.notify(IllegalStateException("Is it me you're looking for?"))
    }
}
