package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration
import com.bugsnag.android.createCustomHeaderDelivery
import com.bugsnag.android.createDefaultDelivery

/**
 * Sends a session which is cached on disk to Bugsnag, then sent on a separate launch,
 * using a custom API client which modifies the request.
 */
internal class CustomClientSessionFlushScenario(config: Configuration,
                                                context: Context) : Scenario(config, context) {

    override fun run() {
        super.run()

        if ("DeliverSessions" == eventMetaData) {
            // simulate activity lifecycle callback occurring before api client can be set
            Bugsnag.startSession()
            config.delivery = createCustomHeaderDelivery(context)
        } else {
            disableAllDelivery()
            Bugsnag.startSession()
        }
    }

}
