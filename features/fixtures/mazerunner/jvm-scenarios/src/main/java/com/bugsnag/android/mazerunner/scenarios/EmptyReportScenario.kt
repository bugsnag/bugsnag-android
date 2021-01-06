package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration
import java.io.File

internal class EmptyReportScenario(
    config: Configuration,
    context: Context,
    eventMetadata: String
) : Scenario(config, context, eventMetadata) {

    init {
        config.autoTrackSessions = false
        val errDir = File(context.cacheDir, "bugsnag-errors")

        if (eventMetadata != "non-crashy") {
            disableAllDelivery(config)
        } else {
            val files = errDir.listFiles()
            files.forEach { it.writeText("") }
        }
    }

    override fun startScenario() {
        super.startScenario()

        if (eventMetadata != "non-crashy") {
            Bugsnag.notify(java.lang.RuntimeException("Whoops"))
        }
    }
}
