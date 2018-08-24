package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.*

/**
 * Sends a session using a custom API client which modifies the request.
 */
internal class CustomClientSessionScenario(config: Configuration,
                                           context: Context) : Scenario(config, context) {

    override fun run() {
        config.delivery = createCustomHeaderDelivery(context)
        super.run()
        Bugsnag.startSession()
        flushAllSessions()
    }

}
