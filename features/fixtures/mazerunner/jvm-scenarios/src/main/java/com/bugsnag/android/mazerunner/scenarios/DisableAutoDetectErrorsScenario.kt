package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.Configuration

/**
 * Attempts to send a handled exception to Bugsnag, when the exception handler is disabled,
 * which should result in no operation.
 */
internal class DisableAutoDetectErrorsScenario(
    config: Configuration,
    context: Context,
    eventMetadata: String
) : Scenario(config, context, eventMetadata) {

    init {
        config.autoTrackSessions = false
        config.enabledErrorTypes.unhandledExceptions = false
    }

    override fun startScenario() {
        super.startScenario()
        throw RuntimeException("Should never appear")
    }
}
