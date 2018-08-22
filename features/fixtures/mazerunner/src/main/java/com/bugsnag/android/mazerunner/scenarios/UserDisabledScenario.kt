package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration

/**
 * Sends a handled exception to Bugsnag, which does not include user data.
 */
internal class UserDisabledScenario(config: Configuration,
                                    context: Context) : Scenario(config, context) {

    override fun run() {
        super.run()
        Bugsnag.setUser(null, null, null)
        Bugsnag.notify(generateException())
    }

}
