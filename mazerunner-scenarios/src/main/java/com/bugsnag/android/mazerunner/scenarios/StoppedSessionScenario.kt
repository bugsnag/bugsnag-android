package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration
import com.bugsnag.android.flushAllSessions
import android.os.Handler
import android.os.HandlerThread
import android.util.Log

/**
 * Sends an exception after stopping the session
 */
internal class StoppedSessionScenario(config: Configuration,
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
            // send 1st exception which should include session info
            client.startSession()
            Log.d("Bugsnag - StoppedSessionScenario", "First session started")
            Thread.sleep(100)
            flushAllSessions()
            Log.d("Bugsnag - StoppedSessionScenario", "First session flushed")
            Thread.sleep(100)
            client.notifyBlocking(generateException())
            Log.d("Bugsnag - StoppedSessionScenario", "First exception sent")
            Thread.sleep(100)

            // send 2nd exception which should not include session info
            client.stopSession()
            Log.d("Bugsnag - StoppedSessionScenario", "First session stopped")
            Thread.sleep(100)
            flushAllSessions()
            Log.d("Bugsnag - StoppedSessionScenario", "First session flushed (again)")
            Thread.sleep(100)
            client.notifyBlocking(generateException())
            Log.d("Bugsnag - StoppedSessionScenario", "Second exception sent")
        }
    }
}
