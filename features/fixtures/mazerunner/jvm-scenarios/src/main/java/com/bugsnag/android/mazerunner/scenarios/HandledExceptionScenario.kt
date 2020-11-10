package com.bugsnag.android.mazerunner.scenarios

import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration

import android.content.Context

/**
 * Sends a handled exception to Bugsnag, which does not include session data.
 */
internal class HandledExceptionScenario(config: Configuration,
                                        context: Context) : Scenario(config, context) {
    init {
        config.autoTrackSessions = false
    }

    override fun run() {
        super.run()
        Bugsnag.notify(generateException())
    }

}
