package com.bugsnag.android.mazerunner.scenarios

import com.bugsnag.android.Bugsnag

/**
 * Attempts to send a handled exception to Bugsnag, when the notifyReleaseStages is an array.
 */
internal class ArrayNotifyReleaseStageScenario : Scenario() {

    override fun run() {
        Bugsnag.setReleaseStage("prod")
        Bugsnag.setNotifyReleaseStages("dev", "prod")
        Bugsnag.notify(generateException())
    }

}
