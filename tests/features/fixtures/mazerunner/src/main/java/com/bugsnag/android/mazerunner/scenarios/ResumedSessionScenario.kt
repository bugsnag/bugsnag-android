package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration
import com.bugsnag.android.flushAllSessions
import android.os.Handler
import android.os.HandlerThread

/**
 * Sends 2 exceptions, 1 before resuming a session, and 1 after resuming a session.
 */
internal class ResumedSessionScenario(config: Configuration,
                                      context: Context) : Scenario(config, context) {
    override fun run() {
        super.run()
        val client = Bugsnag.getClient()
        val thread = HandlerThread("HandlerThread")
        thread.start()

        Handler(thread.looper).post {
            // send 1st exception
            client.startSession()
            Thread.sleep(100)
            flushAllSessions()
            Thread.sleep(100)
            client.notifyBlocking(generateException())
            Thread.sleep(100)

            // send 2nd exception after resuming a session
            client.stopSession()
            Thread.sleep(100)
            client.resumeSession()
            Thread.sleep(100)
            client.notifyBlocking(generateException())
        }
    }
}
