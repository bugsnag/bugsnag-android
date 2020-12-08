package com.bugsnag.android.mazerunner.scenarios

import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration

import android.content.Context

/**
 * Sends a handled exception to Bugsnag, which includes session data.
 */
internal class HandledExceptionSessionScenario(
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
        Bugsnag.notify(generateException())
    }

}
