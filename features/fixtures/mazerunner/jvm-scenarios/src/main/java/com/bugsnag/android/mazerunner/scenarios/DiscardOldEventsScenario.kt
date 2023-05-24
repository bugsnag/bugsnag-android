package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration
import java.io.File
import java.util.Calendar

internal class DiscardOldEventsScenario(
    config: Configuration,
    context: Context,
    eventMetadata: String
) : Scenario(config, context, eventMetadata) {

    init {
        config.launchDurationMillis = 0
    }

    fun setEventFileTimestamp(file: File, timestamp: Long) {
        val name = file.name
        val suffix = name.substringAfter("_")
        val dstFile = File(file.parent, "${timestamp}_$suffix")
        assert(file.renameTo(dstFile))
    }

    fun oldifyEventFiles() {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DATE, -60)
        cal.add(Calendar.MINUTE, -1)
        val timestamp = cal.timeInMillis

        val files = errorsDir().listFiles()
        for (file in files!!) {
            setEventFileTimestamp(file, timestamp)
        }
    }

    override fun startBugsnag(startBugsnagOnly: Boolean) {
        // we override startBugsnag to avoid reporting startup time in this scenario
        // as the time taken frequently pushes the scenario over our timeout
        this.startBugsnagOnly = startBugsnagOnly
        Bugsnag.start(context, config)
    }

    override fun startScenario() {
        super.startScenario()
        Bugsnag.markLaunchCompleted()
        Bugsnag.notify(MyThrowable("DiscardOldEventsScenario"))

        waitForEventFile()
        oldifyEventFiles()

        Bugsnag.notify(MyThrowable("MazeRunner KeepAlive"))
    }

    override fun getInterceptedLogMessages() = listOf("Discarding historical event \\(from .+\\) after failed delivery")
}
