package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration

/**
 * Sends a handled exception to Bugsnag, which includes user data.
 */
internal class UserEnabledScenario(
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
        Bugsnag.notify(generateException())
    }
}
