package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.Configuration
import java.io.File

/**
 * Sends an unhandled exception to Bugsnag.
 */
internal class MinimalUnhandledExceptionScenario(config: Configuration,
                                                 context: Context) : Scenario(config, context) {
    init {
        config.setAutoCaptureSessions(false)
        disableAllDelivery(config)
    }

    override fun run() {
        super.run()
        throw java.lang.IllegalStateException("Whoops")
    }

}
