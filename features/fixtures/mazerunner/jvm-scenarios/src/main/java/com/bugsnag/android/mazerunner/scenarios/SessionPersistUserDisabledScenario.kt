package com.bugsnag.android.mazerunner.scenarios

import android.app.Activity
import android.content.Context
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration

/**
 * Sends a session which is cached on disk to Bugsnag, then sent on a separate launch.
 */
internal class SessionPersistUserDisabledScenario(config: Configuration,
                                   context: Context) : Scenario(config, context) {
    init {
        config.autoTrackSessions = false
        config.persistUser = false
    }

    override fun run() {
        super.run()
        if (eventMetaData != "no_user") {
            Bugsnag.setUser("12345", "test@test.test", "test user")
        }
        Bugsnag.startSession()
    }

}
