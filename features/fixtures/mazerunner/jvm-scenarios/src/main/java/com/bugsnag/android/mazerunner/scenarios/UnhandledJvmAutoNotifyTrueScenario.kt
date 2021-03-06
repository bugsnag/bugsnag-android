package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration
import com.bugsnag.android.setAutoNotify

class UnhandledJvmAutoNotifyTrueScenario(
    config: Configuration,
    context: Context,
    eventMetadata: String?
) : Scenario(config, context, eventMetadata) {

    override fun startScenario() {
        super.startScenario()
        setAutoNotify(Bugsnag.getClient(), false)
        setAutoNotify(Bugsnag.getClient(), true)
        throw generateException()
    }
}
