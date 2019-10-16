package com.bugsnag.android.mazerunner.scenarios

import android.content.Context

import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration

/**
 * Attempts to send a handled exception to Bugsnag, when the enabledReleaseStages is empty.
 */
internal class EmptyEnabledReleaseStageScenario(config: Configuration,
                                               context: Context) : Scenario(config, context) {
    init {
        config.autoCaptureSessions = false
        config.releaseStage = "prod"
        config.enabledReleaseStages = emptyList()
    }

    override fun run() {
        super.run()
        Bugsnag.notify(generateException())
    }

}
