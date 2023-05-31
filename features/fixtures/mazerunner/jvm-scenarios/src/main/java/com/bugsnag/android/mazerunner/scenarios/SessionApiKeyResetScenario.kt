package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration
import com.bugsnag.android.OnSessionCallback

/**
 * Reset session apikey.
 */
internal class SessionApiKeyResetScenario(
    config: Configuration,
    context: Context,
    eventMetadata: String
) : Scenario(config, context, eventMetadata) {

    init {
        config.addOnSession(
            OnSessionCallback { session ->
                session.setApiKey("TEST APIKEY")
                true
            }
        )
    }

    override fun startScenario() {
        super.startScenario()
        Bugsnag.startSession()
    }
}
