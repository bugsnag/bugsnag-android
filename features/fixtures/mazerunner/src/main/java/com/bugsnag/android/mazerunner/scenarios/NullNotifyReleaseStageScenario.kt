package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration

/**
 * Attempts to send a handled exception to Bugsnag, when the notifyReleaseStages is null.
 */
internal class NullNotifyReleaseStageScenario(config: Configuration,
                                              context: Context) : Scenario(config, context) {
    init {
        config.setAutoCaptureSessions(false)
        config.setReleaseStage("prod")
        config.setNotifyReleaseStages(null)
    }

    override fun run() {
        super.run()
        Bugsnag.notify(generateException())
    }

}
