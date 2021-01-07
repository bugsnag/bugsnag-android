package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration

/**
 * Sends an unhandled exception to Bugsnag, which includes session data.
 */
internal class UnhandledExceptionSessionScenario(
    config: Configuration,
    context: Context,
    eventMetadata: String
) : Scenario(config, context, eventMetadata) {

    init {
        config.autoTrackSessions = false
        disableSessionDelivery(config)
    }

    override fun startScenario() {
        super.startScenario()
        Bugsnag.startSession()
        throw generateException()
    }
}
