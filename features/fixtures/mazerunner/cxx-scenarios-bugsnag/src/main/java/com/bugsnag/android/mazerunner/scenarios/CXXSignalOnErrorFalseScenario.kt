package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.Configuration
import com.bugsnag.android.mazerunner.getZeroEventsLogMessages

class CXXSignalOnErrorFalseScenario(
    config: Configuration,
    context: Context,
    eventMetadata: String?
) : Scenario(config, context, eventMetadata) {

    init {
        config.autoTrackSessions = false
        System.loadLibrary("bugsnag-ndk")
        System.loadLibrary("cxx-scenarios-bugsnag")
    }

    external fun crash()

    override fun startScenario() {
        super.startScenario()
        if ("non-crashy" != eventMetadata) {
            crash()
        }
    }

    override fun getInterceptedLogMessages() = getZeroEventsLogMessages(eventMetadata)
}
