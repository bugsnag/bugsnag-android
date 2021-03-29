package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration

/**
 * Sends a handled exception to Bugsnag, which contains metadata that should be filtered
 */
internal class AutoRedactKeysScenario(
    config: Configuration,
    context: Context,
    eventMetadata: String
) : Scenario(config, context, eventMetadata) {

    override fun startScenario() {
        super.startScenario()
        Bugsnag.addMetadata("user", "password", "hunter2")
        Bugsnag.addMetadata("custom", "password", "hunter2")
        Bugsnag.addMetadata("custom", "foo", "hunter2")
        Bugsnag.notify(generateException())
    }
}
