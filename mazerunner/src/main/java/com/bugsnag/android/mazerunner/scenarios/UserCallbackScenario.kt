package com.bugsnag.android.mazerunner.scenarios

import com.bugsnag.android.Bugsnag

/**
 * Sends a handled exception to Bugsnag, which overrides the default user via a callback
 */
internal class UserCallbackScenario : Scenario() {

    override fun run() {
        Bugsnag.setUser("abc", "user@example.com", "Jake")
        Bugsnag.notify(generateException(), {
            it.error?.setUser("Agent Pink", "bob@example.com", "Zebedee")
        })
    }

}
