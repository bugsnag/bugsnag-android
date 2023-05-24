package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration
import com.bugsnag.android.mazerunner.getZeroEventsLogMessages

internal class DiscardBigEventsScenario(
    config: Configuration,
    context: Context,
    eventMetadata: String
) : Scenario(config, context, eventMetadata) {

    init {
        config.launchDurationMillis = 0
        config.addOnSend {
            it.addMetadata("big", "stuff", generateBigText())
            true
        }
    }

    fun generateBigText(): String {
        return "*".repeat(1024 * 1024)
    }

    override fun startBugsnag(startBugsnagOnly: Boolean) {
        this.startBugsnagOnly = startBugsnagOnly
        Bugsnag.start(context, config)

        // Wait and signal to Maze that the error has been deleted
        if (eventMetadata == "delete-wait") {
            waitForNoEventFiles()
            Bugsnag.notify(MyThrowable("ErrorsDirectoryEmpty"))
        }
    }

    override fun startScenario() {
        super.startScenario()
        Bugsnag.markLaunchCompleted()
        Bugsnag.notify(MyThrowable("DiscardBigEventsScenario"))
    }

    override fun getInterceptedLogMessages(): List<String> {
        return getZeroEventsLogMessages(startBugsnagOnly)
    }
}
