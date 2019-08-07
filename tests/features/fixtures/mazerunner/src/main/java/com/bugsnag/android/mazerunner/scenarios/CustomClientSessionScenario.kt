package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration
import com.bugsnag.android.createCustomHeaderDelivery

/**
 * Sends a session using a custom API client which modifies the request.
 */
internal class CustomClientSessionScenario(config: Configuration,
                                           context: Context) : Scenario(config, context) {

    init {
        config.delivery = createCustomHeaderDelivery(config)
    }

    override fun run() {
        super.run()
        Bugsnag.startSession()
    }

}
