package com.bugsnag.android.mazerunner.scenarios

import android.app.Activity
import android.content.Context
import android.content.Intent

import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration
import java.io.File

/**
 * Sends a handled exception to Bugsnag, which does not include session data.
 */
internal class InternalReportScenario(config: Configuration,
                                      context: Context) : Scenario(config, context) {

    init {
        config.setAutoCaptureSessions(false)
        config.beforeSend { true }

        if (context is Activity) {
            eventMetaData = context.intent.getStringExtra("eventMetaData")
            if (eventMetaData != "non-crashy") {
                disableAllDelivery(config)
            } else {
                val files = File(context.cacheDir, "bugsnag-errors").listFiles()
                files.forEach { it.writeText("{[]}") }
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
