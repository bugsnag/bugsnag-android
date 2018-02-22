package com.bugsnag.android.mazerunner.scenarios

import com.bugsnag.android.Bugsnag

/**
 * Attempts to send a handled exception to Bugsnag, when the release stage is set.
 */
internal class InsideReleaseStageScenario : Scenario() {

    override fun run() {
        Bugsnag.setReleaseStage("prod")
        Bugsnag.setNotifyReleaseStages("prod")
        Bugsnag.notify(RuntimeException("InsideReleaseStageScenario"))
    }

}
