package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration

/**
 * Attempts to send a handled exception to Bugsnag, when the enabledReleaseStages is empty.
 */
internal class EmptyEnabledReleaseStageScenario(
    config: Configuration,
    context: Context,
    eventMetadata: String
) : Scenario(config, context, eventMetadata) {

    init {
        config.releaseStage = "prod"
        config.enabledReleaseStages = emptySet()
    }

    override fun startScenario() {
        super.startScenario()
        Bugsnag.notify(generateException())
    }

    override fun getInterceptedLogMessages() = listOf("Bugsnag loaded")
}
