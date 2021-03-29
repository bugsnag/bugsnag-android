package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration

/**
 * Sends a session which is cached on disk to Bugsnag, then sent on a separate launch.
 */
internal class SessionPersistUserScenario(
    config: Configuration,
    context: Context,
    eventMetadata: String
) : Scenario(config, context, eventMetadata) {

    init {
        config.persistUser = true
    }

    override fun startScenario() {
        super.startScenario()
        if (eventMetadata != "no_user") {
            Bugsnag.setUser("12345", "test@test.test", "test user")
        }
        Bugsnag.startSession()
    }
}
