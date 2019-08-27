package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration
import com.bugsnag.android.flushAllSessions
import android.os.Handler
import android.os.HandlerThread
import android.util.Log

/**
 * Sends 2 exceptions, 1 before resuming a session, and 1 after resuming a session.
 */
internal class ResumedSessionScenario(config: Configuration,
                                      context: Context) : Scenario(config, context) {

    init {
        config.autoCaptureSessions = false
    }

    override fun run() {
        super.run()
        val client = Bugsnag.getClient()
        val thread = HandlerThread("HandlerThread")
        thread.start()

        Handler(thread.looper).post {
            // send 1st exception
            client.startSession()
            Log.d("Bugsnag - ResumedSessionScenario", "First session started")
            Thread.sleep(100)
            flushAllSessions()
            Log.d("Bugsnag - ResumedSessionScenario", "First session flushed")
            Thread.sleep(100)
            client.notifyBlocking(generateException())
            Log.d("Bugsnag - ResumedSessionScenario", "First exception sent")
            Thread.sleep(100)

            // send 2nd exception after resuming a session
            client.stopSession()
            Log.d("Bugsnag - ResumedSessionScenario", "First session stopped")
            Thread.sleep(100)
            client.resumeSession()
            Log.d("Bugsnag - ResumedSessionScenario", "First session resumed")
            Thread.sleep(100)
            client.notifyBlocking(generateException())
            Log.d("Bugsnag - ResumedSessionScenario", "Second exception sent")
        }
    }
}
