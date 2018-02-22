package com.bugsnag.android.mazerunner.scenarios

import com.bugsnag.android.Bugsnag

/**
 * Sends a handled exception to Bugsnag, which does not include user data.
 */
internal class UserDisabledScenario : Scenario() {

    override fun run() {
        Bugsnag.setUser(null, null, null)
        Bugsnag.notify(generateException())
    }

}
