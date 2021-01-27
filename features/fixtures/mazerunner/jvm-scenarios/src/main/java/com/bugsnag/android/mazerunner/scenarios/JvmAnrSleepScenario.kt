package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.Configuration
import com.bugsnag.android.mazerunner.createDeadlock

/**
 * Stops the app from responding for a time period
 */
internal class JvmAnrSleepScenario(
    config: Configuration,
    context: Context,
    eventMetadata: String?
) : Scenario(config, context, eventMetadata) {

    init {
        config.autoTrackSessions = false
        config.enabledErrorTypes.anrs = true
    }

    override fun startScenario() {
        super.startScenario()
        createDeadlock()
    }
}
