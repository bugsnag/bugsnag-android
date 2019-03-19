package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration

/**
 * Stops the app from responding for a time period after changing the threshold to be higher
 */
internal class AppNotRespondingLongerThresholdScenario(config: Configuration,
                                  context: Context) : Scenario(config, context) {
    init {
        config.setAutoCaptureSessions(false)
        config.detectAnrs = true
        config.anrThresholdMs = 8000L
    }

    override fun run() {
        super.run()
        Thread.sleep(6000)
    }

}
