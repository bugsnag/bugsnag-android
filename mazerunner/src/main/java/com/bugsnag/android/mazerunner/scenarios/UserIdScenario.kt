package com.bugsnag.android.mazerunner.scenarios

import com.bugsnag.android.Bugsnag

/**
 * Sends a handled exception to Bugsnag, which only includes a user's id
 */
internal class UserIdScenario : Scenario() {

    override fun run() {
        Bugsnag.setUser("abc", null, null)
        Bugsnag.notify(generateException())
    }

}
