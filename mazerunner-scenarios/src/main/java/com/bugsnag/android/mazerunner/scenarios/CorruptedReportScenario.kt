package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration
import java.io.File

/**
 * Verifies that if a report is corrupted, minimal information is still sent to bugsnag.
 */
internal class CorruptedReportScenario(config: Configuration,
                                       context: Context) : Scenario(config, context) {

    init {
        config.setAutoCaptureSessions(false)
        val files = File(context.cacheDir, "bugsnag-errors").listFiles()
        files.forEach { it.writeText("{\"exceptions\":[{\"stacktrace\":[") }

        val nativeFiles = File(context.cacheDir, "bugsnag-native").listFiles()
        nativeFiles.forEach { it.writeText("{\"exceptions\":[{\"stacktrace\":[") }
    }
}
