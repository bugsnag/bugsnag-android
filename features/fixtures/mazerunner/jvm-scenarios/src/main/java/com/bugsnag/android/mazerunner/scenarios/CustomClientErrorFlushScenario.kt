package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.Configuration
import com.bugsnag.android.createCustomHeaderDelivery

/**
 * Sends an unhandled exception which is cached on disk to Bugsnag, then sent on a separate launch,
 * using a custom API client which modifies the request.
 */
internal class CustomClientErrorFlushScenario(
    config: Configuration,
    context: Context,
    eventMetadata: String
) : Scenario(config, context, eventMetadata) {

    init {
        config.autoTrackSessions = false
        if ("online" == eventMetadata) {
            config.delivery = createCustomHeaderDelivery()
        } else {
            disableAllDelivery(config)
        }
    }

    override fun startScenario() {
        super.startScenario()

        if ("online" != eventMetadata) {
            throw RuntimeException("ReportCacheScenario")
        }
    }
}
