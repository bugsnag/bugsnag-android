package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration

/**
 * Stops the app from responding for a time period with ANR detection disabled
 */
internal class AppNotRespondingDisabledScenario(config: Configuration,
                                  context: Context) : Scenario(config, context) {
    init {
        config.setAutoCaptureSessions(false)
        config.detectAnrs = false
    }

    override fun run() {
        super.run()
        Thread.sleep(6000)
    }

}
