package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration

internal class CustomPluginNotifierDescriptionScenario(
    config: Configuration,
    context: Context
) : Scenario(config, context) {

    init {
        config.autoTrackSessions = false
        config.addPlugin(CustomPluginExample())
    }

    override fun run() {
        super.run()
        Bugsnag.notify(RuntimeException())
    }
}
