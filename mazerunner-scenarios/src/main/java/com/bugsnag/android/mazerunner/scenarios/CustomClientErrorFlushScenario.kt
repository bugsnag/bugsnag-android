package com.bugsnag.android.mazerunner.scenarios

import android.app.Activity
import android.content.Context
import com.bugsnag.android.Configuration
import com.bugsnag.android.createCustomHeaderDelivery

/**
 * Sends an unhandled exception which is cached on disk to Bugsnag, then sent on a separate launch,
 * using a custom API client which modifies the request.
 */
internal class CustomClientErrorFlushScenario(config: Configuration,
                                              context: Context) : Scenario(config, context) {
    init {
        config.setAutoTrackSessions(false)
        if (context is Activity) {
            eventMetadata = context.intent.getStringExtra("EVENT_METADATA")
            if ("online" == eventMetadata) {
                config.delivery = createCustomHeaderDelivery()
            } else {
                disableAllDelivery(config)
            }
        }
    }

    override fun run() {
        super.run()

        if ("online" != eventMetadata) {
            throw RuntimeException("ReportCacheScenario")
        }
    }
}
