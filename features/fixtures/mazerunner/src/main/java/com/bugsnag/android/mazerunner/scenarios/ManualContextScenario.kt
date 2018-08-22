package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration
import com.bugsnag.android.mazerunner.scenarios.Scenario

/**
 * Sends a handled exception to Bugsnag, which includes manual context.
 */
internal class ManualContextScenario(config: Configuration,
                                     context: Context) : Scenario(config, context) {

    override fun run() {
        super.run()
        Bugsnag.setContext("FooContext")
        Bugsnag.notify(generateException())
    }

}
