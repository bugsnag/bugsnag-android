package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration
import com.bugsnag.android.OnSendCallback
import java.lang.RuntimeException

internal class OnSendCallbackScenario(
    config: Configuration,
    context: Context,
    eventMetadata: String
) : Scenario(config, context, eventMetadata) {

    init {
        config.addOnSend(
            OnSendCallback { event ->
                event.clearFeatureFlag("deleteMe")
                event.addFeatureFlag(event.featureFlags[0].name, "b")
                event.addMetadata("mazerunner", "onSendCallback", "true")
                event.apiKey = "99999999999999909999999999999999"
                true
            }
        )
    }

    override fun startScenario() {
        super.startScenario()

        Bugsnag.addFeatureFlag("fromStartup", "a")
        Bugsnag.addFeatureFlag("deleteMe")

        if (eventMetadata != "start-only") {
            throw RuntimeException("Unhandled Error")
        }
    }
}
