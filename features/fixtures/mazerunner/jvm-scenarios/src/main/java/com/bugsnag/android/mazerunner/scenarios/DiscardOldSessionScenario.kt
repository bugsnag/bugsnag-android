package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration
import com.bugsnag.android.mazerunner.disableSessionDelivery
import java.io.File
import java.util.Calendar

private const val SESSION_UUID_LENGTH = 36

internal class DiscardOldSessionScenario(
    config: Configuration,
    context: Context,
    eventMetadata: String
) : Scenario(config, context, eventMetadata) {

    init {
        config.launchDurationMillis = 0
        disableSessionDelivery(config)
    }

    fun setSessionFileTimestamp(file: File, timestamp: Long) {
        val name = file.name
        val prefixEnd = name.indexOf('_') + 1 + SESSION_UUID_LENGTH
        val prefix = name.substring(0, prefixEnd)
        val suffix = name.substringAfter(prefix)
        val dstFile = File(file.parent, "${prefix}${timestamp}_${suffix.substringAfter('_')}")
        assert(file.renameTo(dstFile))
    }

    fun sessionDir(): File {
        return File(context.cacheDir, "bugsnag/sessions")
    }

    fun waitForSessionFile() {
        val dir = sessionDir()
        while (dir.listFiles().isNullOrEmpty()) {
            Thread.sleep(100)
        }
    }

    fun oldifySessionFiles() {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DATE, -60)
        cal.add(Calendar.MINUTE, -1)
        val timestamp = cal.timeInMillis

        val files = sessionDir().listFiles()
        for (file in files!!) {
            setSessionFileTimestamp(file, timestamp)
        }
    }

    override fun startScenario() {
        super.startScenario()
        Bugsnag.markLaunchCompleted()
        Bugsnag.startSession()

        waitForSessionFile()
        oldifySessionFiles()

        System.out.println("DiscardOldSessionScenario: Finished oldifying files; sending placeholder event.")
        Bugsnag.notify(MyThrowable("To keep maze-runner from shutting me down prematurely"))
    }
}
