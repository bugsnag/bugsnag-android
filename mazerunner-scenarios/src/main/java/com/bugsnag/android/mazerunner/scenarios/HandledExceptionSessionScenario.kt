package com.bugsnag.android.mazerunner.scenarios

import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration

import android.content.Context

/**
 * Sends a handled exception to Bugsnag, which includes session data.
 */
internal class HandledExceptionSessionScenario(config: Configuration,
                                               context: Context) : Scenario(config, context) {
    init {
        config.autoTrackSessions = false
        disableSessionDelivery(config)
    }

    override fun run() {
        super.run()
        Bugsnag.startSession()
        Bugsnag.notify(generateException())
    }

}
