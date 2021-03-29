package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration
import com.bugsnag.android.mazerunner.createDeadlock

/**
 * Stops the app from responding for a time period
 */
internal class JvmAnrLoopScenario(
    config: Configuration,
    context: Context,
    eventMetadata: String?
) : Scenario(config, context, eventMetadata) {

    init {
        config.enabledErrorTypes.anrs = true
    }

    override fun startScenario() {
        super.startScenario()
        Bugsnag.addMetadata("custom", "global", "present in global metadata")
        Bugsnag.addOnError { event ->
            event.addMetadata("custom", "local", "present in local metadata")
            true
        }
        createDeadlock()
    }
}
