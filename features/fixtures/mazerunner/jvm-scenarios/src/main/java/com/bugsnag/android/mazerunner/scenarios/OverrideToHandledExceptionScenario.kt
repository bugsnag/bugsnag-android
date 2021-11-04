package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration
import com.bugsnag.android.OnErrorCallback

/**
 * Generates an unhandled exception that is overridden so that unhandled is false.
 */
internal class OverrideToHandledExceptionScenario(
    config: Configuration,
    context: Context,
    eventMetadata: String?
) : Scenario(config, context, eventMetadata) {

    init {
        config.addOnError(
            OnErrorCallback {
                it.isUnhandled = false
                true
            }
        )
    }

    override fun startScenario() {
        super.startScenario()
        Bugsnag.startSession()
        throw generateException()
    }
}
