package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.Configuration

internal class CXXConfiguredContextScenario(
    config: Configuration,
    context: Context,
    eventMetadata: String?
) : Scenario(config, context, eventMetadata) {
    init {
        System.loadLibrary("cxx-scenarios-bugsnag")
        config.context = "CustomConfiguredContext"
    }

    external fun crash()

    override fun startScenario() {
        super.startScenario()
        crash()
    }
}
