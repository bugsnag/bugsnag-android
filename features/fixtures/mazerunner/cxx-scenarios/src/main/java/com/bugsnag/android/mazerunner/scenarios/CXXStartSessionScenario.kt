package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration

class CXXStartSessionScenario(
    config: Configuration,
    context: Context,
    eventMetadata: String?
) : Scenario(config, context, eventMetadata) {

    init {
        System.loadLibrary("cxx-scenarios")
        config.autoTrackSessions = false
    }

    external fun crash(counter: Int): Int

    override fun startScenario() {
        super.startScenario()
        Bugsnag.startSession()
        crash(0)
    }
}
