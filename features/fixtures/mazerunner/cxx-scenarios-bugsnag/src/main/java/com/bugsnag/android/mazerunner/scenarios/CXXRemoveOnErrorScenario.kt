package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.Configuration

class CXXRemoveOnErrorScenario(
    config: Configuration,
    context: Context,
    eventMetadata: String?
) : Scenario(config, context, eventMetadata) {

    init {
        System.loadLibrary("bugsnag-ndk")
        System.loadLibrary("cxx-scenarios-bugsnag")
        config.context = "CXXRemoveOnErrorScenario"
    }

    external fun activate()

    override fun startScenario() {
        super.startScenario()
        activate()
    }
}
