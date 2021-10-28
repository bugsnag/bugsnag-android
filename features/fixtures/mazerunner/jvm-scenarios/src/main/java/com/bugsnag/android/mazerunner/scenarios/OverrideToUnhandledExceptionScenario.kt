package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration
import com.bugsnag.android.OnSessionCallback

/**
 * Generates a handled exception that is overridden so that unhandled is true.
 */
internal class OverrideToUnhandledExceptionScenario(
    config: Configuration,
    context: Context,
    eventMetadata: String?
) : Scenario(config, context, eventMetadata) {

    init {
        config.addOnSession(OnSessionCallback { false })
    }

    override fun startScenario() {
        super.startScenario()
        Bugsnag.startSession()
        Bugsnag.notify(generateException()) {
            it.isUnhandled = true
            true
        }
    }
}
