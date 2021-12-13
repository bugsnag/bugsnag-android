package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
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
                event.addMetadata("mazerunner", "onSendCallback", "true")
                event.apiKey = "99999999999999909999999999999999"
                true
            }
        )
    }

    override fun startScenario() {
        super.startScenario()

        if (eventMetadata != "start-only") {
            throw RuntimeException("Unhandled Error")
        }
    }
}
