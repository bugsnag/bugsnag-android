package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import android.content.Intent
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration

/**
 * Sends a manual session payload to Bugsnag.
 */
internal class AutoSessionScenario(config: Configuration,
                                   context: Context) : Scenario(config, context) {

    init {
        config.autoCaptureSessions = true
    }

    override fun run() {
        super.run()
        Bugsnag.init(context, config)
    }

}
