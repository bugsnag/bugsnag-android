package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration

class ExitInfoScenario(
    config: Configuration,
    context: android.content.Context,
    eventMetadata: String?
) : Scenario(config, context, eventMetadata) {
    external fun crash(value: Int): Int
    override fun startScenario() {
        super.startScenario()
        Bugsnag.startSession()
        throw generateException()
    }
}
