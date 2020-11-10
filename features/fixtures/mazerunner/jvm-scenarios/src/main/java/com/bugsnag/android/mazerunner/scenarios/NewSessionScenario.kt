package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import com.bugsnag.android.flushAllSessions

/**
 * Sends an exception after pausing the session
 */
internal class NewSessionScenario(config: Configuration,
                                      context: Context) : Scenario(config, context) {

    companion object {
        private const val SLEEP_MS: Long = 100
    }

    init {
        config.autoTrackSessions = false
    }

    override fun run() {
        super.run()
        val client = Bugsnag.getClient()
        val thread = HandlerThread("HandlerThread")
        thread.start()

        Handler(thread.looper).post(Runnable {
            // send 1st exception which should include session info
            client.startSession()
            Log.d("Bugsnag - New", "First session started")
            Thread.sleep(SLEEP_MS)
            flushAllSessions()
            Log.d("Bugsnag - New", "First session flushed")
            Thread.sleep(SLEEP_MS)
            client.notify(generateException())
            Log.d("Bugsnag - New", "First exception notified")
            Thread.sleep(SLEEP_MS)
            // stop tracking the existing session
            client.pauseSession()
            Log.d("Bugsnag - New", "First session paused")
            Thread.sleep(SLEEP_MS)
            // send 2nd exception which should contain new session info
            client.startSession()
            Log.d("Bugsnag - New", "Second session started")
            Thread.sleep(SLEEP_MS)
            flushAllSessions()
            Log.d("Bugsnag - New", "Second session flushed")
            Thread.sleep(SLEEP_MS)
            client.notify(generateException())
            Log.d("Bugsnag - New", "Second exception notified")
        })
    }
}
