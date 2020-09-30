package com.bugsnag.android.mazerunner.scenarios

import android.content.Context

import com.bugsnag.android.Configuration
import com.bugsnag.android.OnErrorCallback

/**
 * Sends an unhandled exception to Bugsnag where the API key is changed in a callback
 */
internal class UnhandledExceptionApiKeyChangeScenario(config: Configuration,
                                                      context: Context) : Scenario(config, context) {
    init {
        config.autoTrackSessions = false
        config.addOnError(OnErrorCallback { event ->
            event.apiKey = "0000111122223333aaaabbbbcccc9999"
            true
        })
    }

    override fun run() {
        super.run()
        throw generateException()
    }

}
