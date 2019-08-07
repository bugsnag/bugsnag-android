package com.bugsnag.android.mazerunner.scenarios

import android.app.Activity
import android.content.Context
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration
import com.bugsnag.android.createCustomHeaderDelivery

/**
 * Sends an unhandled exception which is cached on disk to Bugsnag, then sent on a separate launch,
 * using a custom API client which modifies the request.
 */
internal class CustomClientErrorFlushScenario(config: Configuration,
                                              context: Context) : Scenario(config, context) {
    init {
        config.setAutoCaptureSessions(false)
        if (context is Activity) {
            eventMetaData = context.intent.getStringExtra("EVENT_METADATA")
            if ("online" == eventMetaData) {
                config.delivery = createCustomHeaderDelivery(config)
            } else {
                disableAllDelivery(config)
            }
        }
    }

    override fun run() {
        super.run()

        if ("online" != eventMetaData) {
            throw RuntimeException("ReportCacheScenario")
        }
    }
}
