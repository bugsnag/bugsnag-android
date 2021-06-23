package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration

/**
 * Attempts to send a handled exception to Bugsnag, when the release stage is null.
 */
internal class NullReleaseStageScenario(
    config: Configuration,
    context: Context,
    eventMetadata: String
) : Scenario(config, context, eventMetadata) {

    init {
        config.releaseStage = null
        config.enabledReleaseStages = setOf("prod")
    }

    override fun startScenario() {
        super.startScenario()
        Bugsnag.notify(generateException())
    }

    override fun getInterceptedLogMessages() = listOf("Bugsnag loaded")
}
