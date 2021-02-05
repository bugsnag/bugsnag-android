package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.Configuration

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

    override fun getInterceptedLogMessages(): List<String> {
        return if ("non-crashy" == eventMetadata) {
            listOf(
                "No startupcrash events to flush to Bugsnag.",
                "No regular events to flush to Bugsnag."
            )
        } else {
            emptyList()
        }
    }
}
