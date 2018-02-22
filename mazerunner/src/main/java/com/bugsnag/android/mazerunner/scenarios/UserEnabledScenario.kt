package com.bugsnag.android.mazerunner.scenarios

import com.bugsnag.android.Bugsnag

/**
 * Sends a handled exception to Bugsnag, which includes user data.
 */
internal class UserEnabledScenario : Scenario() {

    override fun run() {
        Bugsnag.setUser("123", "user@example.com", "Joe Bloggs")
        Bugsnag.notify(RuntimeException("UserEnabledScenario"))
    }

}
