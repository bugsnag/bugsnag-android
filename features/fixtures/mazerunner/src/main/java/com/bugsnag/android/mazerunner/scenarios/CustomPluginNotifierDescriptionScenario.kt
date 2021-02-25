package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Client
import com.bugsnag.android.Configuration
import com.bugsnag.android.CustomPluginExample
import java.lang.RuntimeException


internal class CustomPluginNotifierDescriptionScenario(
    config: Configuration,
    context: Context
) : Scenario(config, context) {

    init {
        CustomPluginExample.register()
        config.setAutoCaptureSessions(false)
    }

    override fun run() {
        super.run()
        Bugsnag.notify(RuntimeException())
    }
}
