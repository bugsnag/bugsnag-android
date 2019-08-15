package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration

/**
 * Attempts to send a handled exception to Bugsnag, when the notifyReleaseStages is empty.
 */
internal class EmptyNotifyReleaseStageScenario(config: Configuration,
                                               context: Context) : Scenario(config, context) {
    init {
        config.setAutoCaptureSessions(false)
        config.setReleaseStage("prod")
        config.setNotifyReleaseStages(emptyList())
    }

    override fun run() {
        super.run()
        Bugsnag.notify(generateException())
    }

}
