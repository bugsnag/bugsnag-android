package com.bugsnag.android.mazerunner.scenarios

import com.bugsnag.android.Bugsnag
import com.bugsnag.android.flushAllSessions

/**
 * Sends a manual session payload to Bugsnag.
 */
internal class ManualSessionScenario : Scenario() {

    override fun run() {
        Bugsnag.setUser("123", "user@example.com", "Joe Bloggs")
        Bugsnag.startSession()
        flushAllSessions()
    }

}
