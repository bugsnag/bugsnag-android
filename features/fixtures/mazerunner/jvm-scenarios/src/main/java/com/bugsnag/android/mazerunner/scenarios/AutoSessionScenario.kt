package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import android.content.Intent
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration

/**
 * Sends a manual session payload to Bugsnag.
 */
internal class AutoSessionScenario(
    config: Configuration,
    context: Context,
    eventMetadata: String
) : Scenario(config, context, eventMetadata) {

    override fun startScenario() {
        super.startScenario()
        config.autoTrackSessions = true
        Bugsnag.start(context, config)
        Bugsnag.setUser("123", "user@example.com", "Joe Bloggs")
        context.startActivity(Intent("com.bugsnag.android.mazerunner.UPDATE_CONTEXT"))
    }
}
