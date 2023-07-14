package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration
import java.util.regex.Pattern

/**
 * Sends a handled exception to Bugsnag, which contains metadata that should be filtered
 */
internal class ManualRedactKeysScenario(
    config: Configuration,
    context: Context,
    eventMetadata: String
) : Scenario(config, context, eventMetadata) {

    init {
        config.redactedKeys = setOf(Pattern.compile(".*foo.*"))
    }

    override fun startScenario() {
        super.startScenario()
        Bugsnag.addMetadata("user", "foo", "hunter2")
        Bugsnag.addMetadata("custom", "foo", "hunter2")
        Bugsnag.addMetadata("custom", "bar", "hunter2")
        Bugsnag.notify(generateException())
    }
}
