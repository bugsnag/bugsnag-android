package com.bugsnag.android.mazerunner.scenarios

import com.bugsnag.android.Bugsnag

/**
 * Attempts to send a handled exception to Bugsnag, when the release stage is not included.
 * This should result in no operation.
 */
internal class OutsideReleaseStageScenario : Scenario() {

    override fun run() {
        Bugsnag.setReleaseStage("prod")
        Bugsnag.setNotifyReleaseStages("dev")
        Bugsnag.notify(RuntimeException("OutsideReleaseStageScenario"))
    }

}
