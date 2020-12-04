package com.bugsnag.android.mazerunner.scenarios

import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration

import android.content.Context

/**
 * Sends a handled exception to Bugsnag where the API key is changed in a callback
 */
internal class HandledExceptionApiKeyChangeScenario(
    config: Configuration,
    context: Context,
    eventMetadata: String
) : Scenario(config, context, eventMetadata) {

    init {
        config.autoTrackSessions = false
    }

    override fun startScenario() {
        super.startScenario()
        Bugsnag.notify(generateException()) { event ->
            event.apiKey = "0000111122223333aaaabbbbcccc9999"
            true
        }
    }
}
