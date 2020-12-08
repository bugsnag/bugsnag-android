package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration
import com.bugsnag.android.mazerunner.BugsnagIntentParams

/**
 * Sends handled exceptions to Bugsnag from two different processes
 */
internal class MultiProcessHandledExceptionScenario(
    config: Configuration,
    context: Context,
    eventMetadata: String
) : Scenario(config, context, eventMetadata) {

    init {
        config.autoTrackSessions = false
    }

    override fun startScenario() {
        super.startScenario()
        Bugsnag.notify(generateException())

        if (!isRunningFromBackgroundService()) {
            val scenarioParams = BugsnagIntentParams(
                javaClass.simpleName,
                config.apiKey,
                config.endpoints.notify,
                config.endpoints.sessions,
                eventMetadata
            )
            launchMultiProcessService(scenarioParams::encode)
        }
    }
}
