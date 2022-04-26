package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration
import com.bugsnag.android.createCustomHeaderDelivery
import com.bugsnag.android.mazerunner.disableAllDelivery

/**
 * Sends an unhandled exception and sessions which is cached on disk to Bugsnag,
 * then sent on a separate launch, using a custom HTTP client which modifies the request.
 */
internal class CustomHttpClientFlushScenario(
    config: Configuration,
    context: Context,
    eventMetadata: String?
) : Scenario(config, context, eventMetadata) {

    override fun startBugsnag(startBugsnagOnly: Boolean) {
        if (startBugsnagOnly) {
            config.delivery = createCustomHeaderDelivery()
        } else {
            disableAllDelivery(config)
        }
        super.startBugsnag(startBugsnagOnly)
    }

    override fun startScenario() {
        super.startScenario()

        Bugsnag.startSession()
        throw RuntimeException("ReportCacheScenario")
    }
}
