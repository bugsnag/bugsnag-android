package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration

/**
 * Sends a handled exception to Bugsnag, which contains metadata that should be filtered
 */
internal class AutoFilterScenario(config: Configuration,
                                  context: Context) : Scenario(config, context) {

    override fun run() {
        super.run()
        Bugsnag.addToTab("user", "password", "hunter2")
        Bugsnag.addToTab("custom", "password", "hunter2")
        Bugsnag.addToTab("custom", "foo", "hunter2")
        Bugsnag.notify(generateException())
    }

}
