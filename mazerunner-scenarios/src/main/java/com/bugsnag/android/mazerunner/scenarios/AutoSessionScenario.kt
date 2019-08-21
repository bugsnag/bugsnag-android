package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import android.content.Intent
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration
import com.bugsnag.android.flushAllSessions

/**
 * Sends a manual session payload to Bugsnag.
 */
internal class AutoSessionScenario(config: Configuration,
                                   context: Context) : Scenario(config, context) {

    override fun run() {
        super.run()
        Bugsnag.init(context, config)
    }

}
