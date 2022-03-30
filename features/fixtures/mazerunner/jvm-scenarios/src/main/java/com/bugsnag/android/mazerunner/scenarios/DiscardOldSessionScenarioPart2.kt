package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration
import com.bugsnag.android.EndpointConfiguration

internal class DiscardOldSessionScenarioPart2(
    config: Configuration,
    context: Context,
    eventMetadata: String
) : Scenario(config, context, eventMetadata) {

    init {
        config.launchDurationMillis = 0
        // We set an endpoint so that attempts to send the session will fail.
        config.endpoints = EndpointConfiguration(config.endpoints.notify, "https://nonexistent.bugsnag.com")
    }

    override fun startScenario() {
        super.startScenario()

        // Give Bugsnag time to try sending the serialized session again.
        Thread.sleep(1000)

        Bugsnag.notify(MyThrowable("To keep maze-runner from shutting me down prematurely"))
    }
}
