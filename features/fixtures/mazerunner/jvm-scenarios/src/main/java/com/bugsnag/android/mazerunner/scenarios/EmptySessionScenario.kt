package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration
import com.bugsnag.android.flushAllSessions
import java.io.File

internal class EmptySessionScenario(
    config: Configuration,
    context: Context,
    eventMetadata: String
) : Scenario(config, context, eventMetadata) {

    override fun startBugsnag(startBugsnagOnly: Boolean) {
        config.autoTrackSessions = false

        val dir = File(context.cacheDir, "bugsnag-sessions")

        if (startBugsnagOnly) {
            val files = dir.listFiles()
            Log.d("Bugsnag", "Empty sessions: $files")
            files.forEach { it.writeText("") }
        } else {
            disableAllDelivery(config)
        }
    }

    override fun startScenario() {
        super.startScenario()

        Bugsnag.startSession()

        val thread = HandlerThread("HandlerThread")
        thread.start()

        Handler(thread.looper).post(
            Runnable {
                flushAllSessions()
            }
        )
    }
}
