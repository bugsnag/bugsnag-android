package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration
import com.bugsnag.android.mazerunner.disableAllDelivery

/**
 * Sends a session which is cached on disk to Bugsnag, then sent on a separate launch.
 */
internal class SessionCacheScenario(
    config: Configuration,
    context: Context,
    eventMetadata: String?
) : Scenario(config, context, eventMetadata) {

    init {
        if (eventMetadata == "offline") {
            disableAllDelivery(config)
        }
    }

    override fun startScenario() {
        super.startScenario()
        Bugsnag.setUser("123", "user@example.com", "Joe Bloggs")
        Bugsnag.startSession()
    }
}
