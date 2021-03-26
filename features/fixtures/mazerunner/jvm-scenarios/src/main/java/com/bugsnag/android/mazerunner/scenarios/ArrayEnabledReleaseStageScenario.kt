package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration

/**
 * Attempts to send a handled exception to Bugsnag, when the enabledReleaseStages is an array.
 */
internal class ArrayEnabledReleaseStageScenario(
    config: Configuration,
    context: Context,
    eventMetadata: String
) : Scenario(config, context, eventMetadata) {

    init {
        config.releaseStage = "prod"
        config.enabledReleaseStages = setOf("dev", "prod")
    }

    override fun startScenario() {
        super.startScenario()
        Bugsnag.notify(generateException())
    }
}
