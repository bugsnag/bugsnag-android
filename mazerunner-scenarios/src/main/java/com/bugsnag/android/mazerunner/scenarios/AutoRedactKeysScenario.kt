package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration

/**
 * Sends a handled exception to Bugsnag, which contains metadata that should be filtered
 */
internal class AutoRedactKeysScenario(config: Configuration,
                                      context: Context) : Scenario(config, context) {
    init {
        config.autoTrackSessions = false
    }

    override fun run() {
        super.run()
        Bugsnag.addMetadata("user", "password", "hunter2")
        Bugsnag.addMetadata("custom", "password", "hunter2")
        Bugsnag.addMetadata("custom", "foo", "hunter2")
        Bugsnag.notify(generateException())
    }

}
