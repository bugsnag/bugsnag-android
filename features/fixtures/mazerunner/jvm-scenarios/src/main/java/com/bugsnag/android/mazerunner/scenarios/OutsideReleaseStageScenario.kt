package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration

/**
 * Attempts to send a handled exception to Bugsnag, when the release stage is not included.
 * This should result in no operation.
 */
internal class OutsideReleaseStageScenario(
    config: Configuration,
    context: Context,
    eventMetadata: String
) : Scenario(config, context, eventMetadata) {

    init {
        config.releaseStage = "prod"
        config.enabledReleaseStages = setOf("dev")
    }

    override fun startScenario() {
        super.startScenario()
        Bugsnag.notify(RuntimeException("OutsideReleaseStageScenario"))
    }

    override fun getInterceptedLogMessages() = listOf("Bugsnag loaded")
}
