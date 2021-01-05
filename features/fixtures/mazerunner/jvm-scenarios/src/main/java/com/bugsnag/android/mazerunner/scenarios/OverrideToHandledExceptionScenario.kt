package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration
import com.bugsnag.android.OnErrorCallback
import com.bugsnag.android.OnSessionCallback

/**
 * Generates an unhandled exception that is overridden so that unhandled is false.
 */
internal class OverrideToHandledExceptionScenario(
    config: Configuration,
    context: Context
) : Scenario(config, context) {
    init {
        config.autoTrackSessions = true
        config.addOnError(
            OnErrorCallback {
                it.isUnhandled = false
                true
            }
        )
        config.addOnSession(OnSessionCallback { false })
    }

    override fun run() {
        super.run()
        Bugsnag.startSession()
        if (eventMetaData != "non-crashy") {
            throw generateException()
        }
    }
}
