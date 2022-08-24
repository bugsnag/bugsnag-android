package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.Configuration

class CXXExceptionWithUsageScenario(
    config: Configuration,
    context: Context,
    eventMetadata: String?
) : Scenario(config, context, eventMetadata) {

    companion object {
        init {
            System.loadLibrary("cxx-scenarios")
        }
    }

    init {
        config.maxBreadcrumbs = 10
        config.autoTrackSessions = false
    }

    external fun crash()

    override fun startScenario() {
        super.startScenario()
        crash()
    }
}
