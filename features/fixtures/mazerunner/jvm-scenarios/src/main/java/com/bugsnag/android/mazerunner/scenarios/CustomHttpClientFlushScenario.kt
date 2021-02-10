package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration
import com.bugsnag.android.createCustomHeaderDelivery

/**
 * Sends an unhandled exception and sessions which is cached on disk to Bugsnag,
 * then sent on a separate launch, using a custom HTTP client which modifies the request.
 */
internal class CustomHttpClientFlushScenario(
    config: Configuration,
    context: Context,
    eventMetadata: String
) : Scenario(config, context, eventMetadata) {

    init {
        config.autoTrackSessions = false

        if ("non-crashy" == eventMetadata) {
            config.delivery = createCustomHeaderDelivery()
        } else {
            disableAllDelivery(config)
        }
    }

    override fun startScenario() {
        super.startScenario()

        if ("non-crashy" != eventMetadata) {
            Bugsnag.startSession()
            throw RuntimeException("ReportCacheScenario")
        }
    }
}
