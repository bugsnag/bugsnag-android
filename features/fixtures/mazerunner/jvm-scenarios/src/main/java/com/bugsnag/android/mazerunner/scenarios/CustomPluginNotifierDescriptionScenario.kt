package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration

internal class CustomPluginNotifierDescriptionScenario(
    config: Configuration,
    context: Context,
    eventMetadata: String
) : Scenario(config, context, eventMetadata) {

    init {
        config.addPlugin(CustomPluginExample())
    }

    override fun startScenario() {
        super.startScenario()
        Bugsnag.notify(RuntimeException())
    }
}
