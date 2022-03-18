package com.bugsnag.android.mazerunner.scenarios

import java.io.File
import java.util.Date
import android.content.Context
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration

internal class DiscardBigEventsScenario(
    config: Configuration,
    context: Context,
    eventMetadata: String
) : Scenario(config, context, eventMetadata) {

    override fun startScenario() {
        super.startScenario()
        Bugsnag.notify(MyThrowable("DiscardBigEventsScenario"))
        Thread.sleep(1000)
        val files = File(context.cacheDir, "bugsnag-errors").listFiles()
        val sixtyDaysAgo = 60L * 24 * 60 * 60 * 1000
        val timestamp = Date().time - sixtyDaysAgo - 1
        for (file in files!!) {
            file.setLastModified(timestamp)
        }
    }
}
