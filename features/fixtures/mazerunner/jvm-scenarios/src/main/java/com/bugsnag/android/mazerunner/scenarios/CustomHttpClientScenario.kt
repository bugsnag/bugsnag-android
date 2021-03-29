package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration
import com.bugsnag.android.createCustomHeaderDelivery

/**
 * Sends a handled exception and sessions to Bugsnag using a custom
 * HTTP client which modifies the request.
 */
internal class CustomHttpClientScenario(
    config: Configuration,
    context: Context,
    eventMetadata: String
) : Scenario(config, context, eventMetadata) {

    init {
        config.delivery = createCustomHeaderDelivery()
    }

    override fun startScenario() {
        super.startScenario()
        Bugsnag.startSession()
        Bugsnag.notify(RuntimeException("Hello"))
    }
}
