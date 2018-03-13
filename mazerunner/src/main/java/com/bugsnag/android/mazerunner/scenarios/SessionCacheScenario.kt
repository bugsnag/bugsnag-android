package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration

/**
 * Sends a session which is cached on disk to Bugsnag, then sent on a separate launch.
 */
internal class SessionCacheScenario(config: Configuration,
                                   context: Context) : Scenario(config, context) {

    override fun run() {
        super.run()
        Bugsnag.setUser("123", "user@example.com", "Joe Bloggs")
        Bugsnag.startSession()
        disableSessionDelivery()
    }

}
