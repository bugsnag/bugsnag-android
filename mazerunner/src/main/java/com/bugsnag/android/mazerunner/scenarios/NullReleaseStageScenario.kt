package com.bugsnag.android.mazerunner.scenarios

import com.bugsnag.android.Bugsnag

/**
 * Attempts to send a handled exception to Bugsnag, when the release stage is null.
 */
internal class NullReleaseStageScenario : Scenario() {

    override fun run() {
        Bugsnag.setReleaseStage(null)
        Bugsnag.setNotifyReleaseStages("prod")
        Bugsnag.notify(generateException())
    }

}
