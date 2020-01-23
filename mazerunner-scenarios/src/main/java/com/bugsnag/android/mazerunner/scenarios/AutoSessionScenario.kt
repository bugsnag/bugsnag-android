package com.bugsnag.android.mazerunner.scenarios

import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration

import android.content.Context
import android.content.Intent

/**
 * Sends a manual session payload to Bugsnag.
 */
internal class AutoSessionScenario(config: Configuration,
                                   context: Context) : Scenario(config, context) {
    override fun run() {
        super.run()
        config.autoTrackSessions = true
        Bugsnag.start(context, config)
        Bugsnag.setUser("123", "user@example.com", "Joe Bloggs")
        context.startActivity(Intent("com.bugsnag.android.mazerunner.UPDATE_CONTEXT"))
    }

}
