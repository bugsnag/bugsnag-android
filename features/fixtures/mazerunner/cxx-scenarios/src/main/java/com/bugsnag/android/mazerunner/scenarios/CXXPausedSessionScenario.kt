package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration

class CXXPausedSessionScenario(
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
        if ("non-crashy" != eventMetadata) {
            Bugsnag.startSession()
            Bugsnag.pauseSession()
            crash(0)
        }
    }
}
