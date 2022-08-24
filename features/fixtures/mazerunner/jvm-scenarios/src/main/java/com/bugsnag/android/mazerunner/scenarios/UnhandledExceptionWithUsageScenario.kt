package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.Configuration

class UnhandledExceptionWithUsageScenario(
    config: Configuration,
    context: Context,
    eventMetadata: String
) : Scenario(config, context, eventMetadata) {

    init {
        config.maxBreadcrumbs = 10
        config.autoTrackSessions = false
    }

    override fun startScenario() {
        super.startScenario()
        throw generateException()
    }
}
