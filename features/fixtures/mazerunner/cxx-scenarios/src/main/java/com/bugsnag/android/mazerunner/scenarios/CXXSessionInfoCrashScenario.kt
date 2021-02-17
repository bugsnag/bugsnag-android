package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration

class CXXSessionInfoCrashScenario(
    config: Configuration,
    context: Context,
    eventMetadata: String?
) : Scenario(config, context, eventMetadata) {

    init {
        System.loadLibrary("cxx-scenarios")
        config.autoTrackSessions = false
    }

    external fun crash(value: Int): Int

    override fun startScenario() {
        super.startScenario()
        Bugsnag.startSession()
        Bugsnag.notify(Exception("For the first"))
        Bugsnag.notify(Exception("For the second"))
        crash(3837)
    }
}
