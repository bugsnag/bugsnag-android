package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration

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

    override fun startScenario() {
        super.startScenario()
        Bugsnag.markLaunchCompleted()
        Bugsnag.notify(MyThrowable("DiscardBigEventsScenario"))
    }
}
