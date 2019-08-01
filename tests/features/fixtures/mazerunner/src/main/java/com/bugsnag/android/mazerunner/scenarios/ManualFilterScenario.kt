package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration

/**
 * Sends a handled exception to Bugsnag, which contains metadata that should be filtered
 */
internal class ManualFilterScenario(config: Configuration,
                                    context: Context) : Scenario(config, context) {
    init {
        config.setFilters(arrayOf("foo"))
    }

    override fun run() {
        super.run()
        Bugsnag.addToTab("user", "foo", "hunter2")
        Bugsnag.addToTab("custom", "foo", "hunter2")
        Bugsnag.addToTab("custom", "bar", "hunter2")
        Bugsnag.notify(generateException())
    }

}
