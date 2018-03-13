package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration

/**
 * Sends a handled exception to Bugsnag, which only includes a user's email
 */
internal class UserEmailScenario(config: Configuration,
                                 context: Context) : Scenario(config, context) {

    override fun run() {
        super.run()
        Bugsnag.setUser(null, "user@example.com", null)
        Bugsnag.notify(generateException())
    }

}
