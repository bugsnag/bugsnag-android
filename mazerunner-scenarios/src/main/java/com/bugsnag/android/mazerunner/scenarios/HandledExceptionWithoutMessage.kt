package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration
import com.bugsnag.android.mazerunner.SomeException

/**
 * Sends a handled exception to Bugsnag, which does not include session data.
 */
internal class HandledExceptionWithoutMessageScenario(config: Configuration,
                                                      context: Context) : Scenario(config, context) {
    init {
        config.autoTrackSessions = false
    }

    override fun run() {
        super.run()
        Bugsnag.notify(SomeException())
    }
}
