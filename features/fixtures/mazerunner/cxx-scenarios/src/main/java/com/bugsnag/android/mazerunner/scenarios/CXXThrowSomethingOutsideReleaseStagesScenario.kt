package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.Configuration

class CXXThrowSomethingOutsideReleaseStagesScenario(
    config: Configuration,
    context: Context,
    eventMetadata: String?
) : Scenario(config, context, eventMetadata) {

    /**
     * Sets custom enabled release stages.
     */
    init {
        config.autoTrackSessions = false
        config.enabledReleaseStages = setOf("fee-fi-fo-fum")
        System.loadLibrary("cxx-scenarios")
    }

    external fun crash(num: Int)

    override fun startScenario() {
        super.startScenario()
        crash(23)
    }
}
