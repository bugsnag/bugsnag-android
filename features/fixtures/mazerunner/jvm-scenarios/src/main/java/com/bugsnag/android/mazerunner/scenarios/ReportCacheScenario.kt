package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.Configuration

/**
 * Sends an unhandled exception which is cached on disk to Bugsnag, then sent on a separate launch.
 */
internal class ReportCacheScenario(
    config: Configuration,
    context: Context,
    eventMetadata: String?
) : Scenario(config, context, eventMetadata) {

    init {
        config.autoTrackSessions = false
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
