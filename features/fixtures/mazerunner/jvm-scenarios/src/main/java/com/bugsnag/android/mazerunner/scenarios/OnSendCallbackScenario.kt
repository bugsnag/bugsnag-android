package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.Configuration
import com.bugsnag.android.TestOnSendCallback
import java.lang.RuntimeException

internal class OnSendCallbackScenario(
    config: Configuration,
    context: Context,
    eventMetadata: String
) : Scenario(config, context, eventMetadata) {
    override fun startBugsnag(startBugsnagOnly: Boolean) {
        TestOnSendCallback().register(config)
        super.startBugsnag(startBugsnagOnly)
    }

    override fun startScenario() {
        super.startScenario()

        if (eventMetadata != "start-only") {
            throw RuntimeException("Unhandled Error")
        }
    }
}
