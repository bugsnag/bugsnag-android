package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration
import com.bugsnag.android.OnSendCallback

internal class HandledOnSendCallbackScenario(
    config: Configuration,
    context: Context,
    eventMetadata: String
) : Scenario(config, context, eventMetadata) {

    init {
        config.addOnSend(
            OnSendCallback { event ->
                event.addMetadata("mazerunner", "onSendCallback", "true")
                event.apiKey = "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbba"
                true
            }
        )
    }

    override fun startScenario() {
        super.startScenario()
        Bugsnag.notify(generateException())
    }
}
