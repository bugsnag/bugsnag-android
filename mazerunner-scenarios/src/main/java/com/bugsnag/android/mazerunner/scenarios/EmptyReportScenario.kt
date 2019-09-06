package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.BeforeSend
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration
import java.io.File

/**
 * Verifies that if a report is empty and a beforeSend callback is set,
 * minimal information is still sent to bugsnag.
 */
internal class EmptyReportScenario(config: Configuration,
                                   context: Context) : Scenario(config, context) {

    init {
        config.setAutoCaptureSessions(false)
        config.addBeforeSend(BeforeSend { true })
        val files = File(context.cacheDir, "bugsnag-errors").listFiles()
        files.forEach { it.writeText("") }
    }
}
