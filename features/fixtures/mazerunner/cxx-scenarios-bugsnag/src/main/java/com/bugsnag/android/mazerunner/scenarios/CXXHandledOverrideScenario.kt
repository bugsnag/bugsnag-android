package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration

internal class CXXHandledOverrideScenario(
    config: Configuration,
    context: Context,
    eventMetadata: String?
) : Scenario(config, context, eventMetadata) {

    init {
        System.loadLibrary("cxx-scenarios-bugsnag")
    }

    external fun activate()

    override fun startScenario() {
        super.startScenario()

        Bugsnag.startSession()
        activate()
    }
}
