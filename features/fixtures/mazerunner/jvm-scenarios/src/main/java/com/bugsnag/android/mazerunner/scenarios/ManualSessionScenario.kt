package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration
import com.bugsnag.android.flushAllSessions

/**
 * Sends a manual session payload to Bugsnag.
 */
internal class ManualSessionScenario(
    config: Configuration,
    context: Context,
    eventMetadata: String
) : Scenario(config, context, eventMetadata) {

    init {
        config.autoTrackSessions = false
    }

    override fun startScenario() {
        super.startScenario()
        Bugsnag.setUser("123", "user@example.com", "Joe Bloggs")
        Bugsnag.startSession()
        flushAllSessions()
    }
}
