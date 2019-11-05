package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration

/**
 * Sends a handled exception to Bugsnag, which contains metadata that should be redacted
 */
internal class ManualRedactKeysScenario(config: Configuration,
                                        context: Context) : Scenario(config, context) {
    init {
        config.autoTrackSessions = false
        config.redactKeys = setOf("foo")
    }

    override fun run() {
        super.run()
        Bugsnag.addMetadata("user", "foo", "hunter2")
        Bugsnag.addMetadata("custom", "foo", "hunter2")
        Bugsnag.addMetadata("custom", "bar", "hunter2")
        Bugsnag.notify(generateException())
    }

}
