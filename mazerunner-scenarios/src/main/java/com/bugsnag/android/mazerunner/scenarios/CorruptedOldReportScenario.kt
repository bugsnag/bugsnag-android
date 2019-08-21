package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration
import java.io.File

/**
 * Verifies that if a report is corrupted with an old filename,
 * Bugsnag does not crash.
 */
internal class CorruptedOldReportScenario(config: Configuration,
                                          context: Context) : Scenario(config, context) {

    init {
        config.setAutoCaptureSessions(false)
        val files = File(context.cacheDir, "bugsnag-errors").listFiles()

        // create an empty (invalid) file with an old name
        files.forEach {
            val dir = File(it.parent)
            it.writeText("{\"exceptions\":[{\"stacktrace\":[")
            it.renameTo(File(dir, "1504255147933_683c6b92-b325-4987-80ad-77086509ca1e.json"))
        }
    }

    override fun run() {
        super.run()
        Bugsnag.notify(generateException())
    }
}
