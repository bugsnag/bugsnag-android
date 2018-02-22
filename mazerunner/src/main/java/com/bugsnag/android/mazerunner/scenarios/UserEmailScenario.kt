package com.bugsnag.android.mazerunner.scenarios

import com.bugsnag.android.Bugsnag

/**
 * Sends a handled exception to Bugsnag, which only includes a user's email
 */
internal class UserEmailScenario : Scenario() {

    override fun run() {
        Bugsnag.setUser(null, "user@example.com", null)
        Bugsnag.notify(generateException())
    }

}
