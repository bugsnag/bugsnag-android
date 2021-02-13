package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.Configuration

class CXXTrapOutsideReleaseStagesScenario(
    config: Configuration,
    context: Context,
    eventMetadata: String?
) : Scenario(config, context, eventMetadata) {

    init {
        config.autoTrackSessions = false
        config.enabledReleaseStages = setOf("fee-fi-fo-fum")
        System.loadLibrary("cxx-scenarios")
    }

    external fun crash()

    override fun startScenario() {
        super.startScenario()
        crash()
    }
}
