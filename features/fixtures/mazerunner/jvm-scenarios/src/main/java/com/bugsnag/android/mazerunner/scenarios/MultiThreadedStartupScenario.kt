package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration
import kotlin.concurrent.thread

class MultiThreadedStartupScenario(
    config: Configuration,
    context: Context,
    eventMetadata: String?
) : Scenario(config, context, eventMetadata) {
    override fun startBugsnag(startBugsnagOnly: Boolean) {}

    override fun startScenario() {
        // MIN_PRIORITY = trying our best to be slow without putting delays in place
        val startThread = thread {
            Bugsnag.start(context, config)
        }

        thread {
            try {
                Bugsnag.leaveBreadcrumb("I'm leaving a breadcrumb on another thread")
            } catch (e: Exception) {
                Bugsnag.start(context, config)
                Bugsnag.notify(e)
            }
        }

        // make sure we wait before returning
        startThread.join()
    }
}