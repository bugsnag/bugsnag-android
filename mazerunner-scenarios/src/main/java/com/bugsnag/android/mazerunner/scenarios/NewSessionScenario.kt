package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import com.bugsnag.android.flushAllSessions

/**
 * Sends an exception after stopping the session
 */
internal class NewSessionScenario(config: Configuration,
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
            Log.d("Bugsnag - NewSessionScenario", "First session started")
            Thread.sleep(100)
            flushAllSessions()
            Log.d("Bugsnag - NewSessionScenario", "First session flushed")
            Thread.sleep(100)
            client.notifyBlocking(generateException())
            Log.d("Bugsnag - NewSessionScenario", "First exception notified")
            Thread.sleep(100)
            // stop tracking the existing session
            client.stopSession()
            Log.d("Bugsnag - NewSessionScenario", "First session stopped")
            Thread.sleep(100)
            // send 2nd exception which should contain new session info
            client.startSession()
            Log.d("Bugsnag - NewSessionScenario", "Second session started")
            Thread.sleep(100)
            flushAllSessions()
            Log.d("Bugsnag - NewSessionScenario", "Second session flushed")
            Thread.sleep(100)
            client.notifyBlocking(generateException())
            Log.d("Bugsnag - NewSessionScenario", "Second exception notified")
        }
    }
}
