package com.bugsnag.android.mazerunner.scenarios

import android.content.Context

import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration

/**
 * Sends a handled exception to Bugsnag, which overrides the default user via a callback
 */
internal class UserCallbackScenario(config: Configuration,
                                    context: Context) : Scenario(config, context) {
    init {
        config.autoTrackSessions = false
    }

    override fun run() {
        super.run()
        Bugsnag.setUser("abc", "user@example.com", "Jake")
        Bugsnag.notify(generateException()) {
            it.setUser("Agent Pink", "bob@example.com", "Zebedee")
            true
        }
    }

}
