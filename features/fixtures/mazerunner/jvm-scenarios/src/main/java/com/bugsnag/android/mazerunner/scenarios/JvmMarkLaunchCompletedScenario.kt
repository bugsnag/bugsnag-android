package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration

/**
 * Sends a JVM error to Bugsnag after markLaunchCompleted() is invoked.
 */
internal class JvmMarkLaunchCompletedScenario(
    config: Configuration,
    context: Context,
    eventMetadata: String?
) : Scenario(config, context, eventMetadata) {

    init {
        config.launchDurationMillis = 0
    }

    override fun startScenario() {
        super.startScenario()
        Bugsnag.notify(RuntimeException("first error"))
        Bugsnag.markLaunchCompleted()
        Bugsnag.notify(generateException())
    }
}
