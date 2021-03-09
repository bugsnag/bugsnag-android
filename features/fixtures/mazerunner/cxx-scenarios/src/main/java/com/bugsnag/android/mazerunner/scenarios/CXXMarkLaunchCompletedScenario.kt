package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration

/**
 * Sends an NDK error to Bugsnag after markLaunchCompleted() is invoked.
 */
internal class CXXMarkLaunchCompletedScenario(
    config: Configuration,
    context: Context,
    eventMetadata: String?
) : Scenario(config, context, eventMetadata) {

    external fun crash()

    init {
        config.autoTrackSessions = false
        config.launchDurationMillis = 0
        System.loadLibrary("cxx-scenarios")
    }

    override fun startScenario() {
        super.startScenario()
        Bugsnag.notify(RuntimeException("first error"))
        Bugsnag.markLaunchCompleted()
        crash()
    }
}
