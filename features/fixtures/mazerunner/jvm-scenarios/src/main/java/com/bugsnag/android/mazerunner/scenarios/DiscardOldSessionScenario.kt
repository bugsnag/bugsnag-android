package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration
import com.bugsnag.android.EndpointConfiguration
import com.bugsnag.android.mazerunner.log
import java.io.File
import java.util.Calendar

internal class DiscardOldSessionScenario(
    config: Configuration,
    context: Context,
    eventMetadata: String
) : Scenario(config, context, eventMetadata) {

    init {
        config.launchDurationMillis = 0
        // We set an endpoint so that attempts to send the session will fail.
        config.endpoints = EndpointConfiguration(config.endpoints.notify, "https://nonexistent.bugsnag.com")
    }

    fun setSessionFileTimestamp(file: File, timestamp: Long) {
        val name = file.name
        val uuid = name.substring(0, 35)
        val suffix = name.substringAfter("_")
        val dstFile = File(file.parent, "${uuid}${timestamp}_$suffix")
        assert(file.renameTo(dstFile))
    }

    fun sessionDir(): File {
        log("Waiting for session folder to be created")

        var folder: File? = null
        while (folder == null) {
            Thread.sleep(100)
            folder = File(context.cacheDir, "bugsnag/sessions")
        }
        log("Session folder has been created")

        return folder
    }

    fun waitForSessionFile() {
        val dir = sessionDir()
        log("Waiting for session files to be created")
        while (dir.listFiles()!!.isEmpty()) {
            Thread.sleep(100)
        }
        log("Session files have been created")
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
