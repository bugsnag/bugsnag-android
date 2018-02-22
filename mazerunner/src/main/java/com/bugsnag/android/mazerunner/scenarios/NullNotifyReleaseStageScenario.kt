package com.bugsnag.android.mazerunner.scenarios

import com.bugsnag.android.Bugsnag

/**
 * Attempts to send a handled exception to Bugsnag, when the notifyReleaseStages is null.
 */
internal class NullNotifyReleaseStageScenario : Scenario() {

    override fun run() {
        Bugsnag.setReleaseStage("prod")
//        Bugsnag.setNotifyReleaseStages()
        Bugsnag.notify(generateException())
    }

}
