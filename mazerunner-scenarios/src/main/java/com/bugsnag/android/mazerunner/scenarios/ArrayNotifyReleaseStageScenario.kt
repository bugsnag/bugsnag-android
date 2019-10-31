package com.bugsnag.android.mazerunner.scenarios

import android.content.Context

import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration

/**
 * Attempts to send a handled exception to Bugsnag, when the enabledReleaseStages is an array.
 */
internal class ArrayEnabledReleaseStageScenario(config: Configuration,
                                               context: Context) : Scenario(config, context) {
    init {
        config.autoTrackSessions = false
        config.releaseStage = "prod"
        config.enabledReleaseStages = setOf("dev", "prod")
    }

    override fun run() {
        super.run()
        Bugsnag.notify(generateException())
    }

}
