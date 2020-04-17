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
            // send 1st exception
            client.startSession()
            Log.d("Bugsnag - Resumed", "First session started")
            Thread.sleep(SLEEP_MS)
            flushAllSessions()
            Log.d("Bugsnag - Resumed", "First session flushed")
            Thread.sleep(SLEEP_MS)
            client.notify(generateException())
            Log.d("Bugsnag - Resumed", "First exception sent")
            Thread.sleep(SLEEP_MS)

            // send 2nd exception after resuming a session
            client.pauseSession()
            Log.d("Bugsnag - Resumed", "First session paused")
            Thread.sleep(SLEEP_MS)
            client.resumeSession()
            Log.d("Bugsnag - Resumed", "First session resumed")
            Thread.sleep(SLEEP_MS)
            client.notify(generateException())
            Log.d("Bugsnag - Resumed", "Second exception sent")
        })
    }
}
