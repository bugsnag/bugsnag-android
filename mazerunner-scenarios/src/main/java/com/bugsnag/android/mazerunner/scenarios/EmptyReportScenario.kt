package com.bugsnag.android.mazerunner.scenarios

import android.app.Activity
import android.content.Context

import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration
import java.io.File

internal class EmptyReportScenario(config: Configuration,
                                   context: Context) : Scenario(config, context) {

    init {
        config.autoTrackSessions = false

        if (context is Activity) {
            eventMetaData = context.intent.getStringExtra("EVENT_METADATA")
            val errDir = File(context.cacheDir, "bugsnag-errors")

            if (eventMetaData != "non-crashy") {
                disableAllDelivery(config)
            } else {
                val files = errDir.listFiles()
                files.forEach { it.writeText("") }
            }
        }
    }

    override fun run() {
        super.run()

        if (eventMetaData != "non-crashy") {
            Bugsnag.notify(java.lang.RuntimeException("Whoops"))
        }
    }
}
