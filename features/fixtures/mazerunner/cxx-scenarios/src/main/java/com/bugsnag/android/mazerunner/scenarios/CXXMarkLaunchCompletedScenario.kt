package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import android.os.Handler
import android.os.Looper
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
        config.launchDurationMillis = 0
        System.loadLibrary("cxx-scenarios")

        config.addOnSend { _ ->
            Handler(Looper.getMainLooper()).post {
                crash()
            }

            true
        }
    }

    override fun startScenario() {
        super.startScenario()
        Bugsnag.notify(RuntimeException("first error"))
        Bugsnag.markLaunchCompleted()
    }
}
