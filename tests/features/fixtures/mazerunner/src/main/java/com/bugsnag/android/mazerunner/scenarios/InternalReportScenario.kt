package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration
import java.io.File

/**
 * Sends a handled exception to Bugsnag, which does not include session data.
 */
internal class InternalReportScenario(config: Configuration,
                                      context: Context) : Scenario(config, context) {

    init {
        config.setAutoCaptureSessions(false)
        disableAllDelivery(config)
    }

    override fun run() {
        super.run()
        Bugsnag.notify(java.lang.RuntimeException("Whoops"))
    }

}