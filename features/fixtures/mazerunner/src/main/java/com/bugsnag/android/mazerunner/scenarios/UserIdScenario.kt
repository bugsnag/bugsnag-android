package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration

/**
 * Sends a handled exception to Bugsnag, which only includes a user's id
 */
internal class UserIdScenario(config: Configuration,
                              context: Context) : Scenario(config, context) {

    override fun run() {
        super.run()
        Bugsnag.setUser("abc", null, null)
        Bugsnag.notify(generateException())
    }

}
