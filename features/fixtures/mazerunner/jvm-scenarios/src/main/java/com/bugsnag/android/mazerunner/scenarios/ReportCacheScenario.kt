package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.Configuration
import com.bugsnag.android.mazerunner.disableAllDelivery

/**
 * Sends an unhandled exception which is cached on disk to Bugsnag, then sent on a separate launch.
 */
internal class ReportCacheScenario(
    config: Configuration,
    context: Context,
    eventMetadata: String?
) : Scenario(config, context, eventMetadata) {

    init {
        if (eventMetadata != "online") {
            disableAllDelivery(config)
        }
    }

    override fun startScenario() {
        super.startScenario()
        if (eventMetadata != "online") {
            throw RuntimeException("ReportCacheScenario")
        }
    }
}
