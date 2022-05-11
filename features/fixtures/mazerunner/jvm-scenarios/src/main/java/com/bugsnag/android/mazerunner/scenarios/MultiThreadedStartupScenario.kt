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
        val startThread = thread(name = "AsyncStart") {
            Bugsnag.start(context, config)
        }

        thread(name = "leaveBreadcrumb") {
            // simulate the start of some startup work, but not enough for Bugsnag.start to complete
            Thread.sleep(1L)
            try {
                Bugsnag.leaveBreadcrumb("I'm leaving a breadcrumb on another thread")
                Bugsnag.notify(Exception("Scenario complete"))
            } catch (e: Exception) {
                Bugsnag.start(context, config)
                Bugsnag.notify(e)
            }
        }

        // make sure we wait before returning
        startThread.join()
    }
}
