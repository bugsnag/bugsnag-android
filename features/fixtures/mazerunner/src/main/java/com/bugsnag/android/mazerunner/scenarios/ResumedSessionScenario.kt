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
    init {
        config.setAutoCaptureSessions(false)
    }

    override fun run() {
        super.run()
        val client = Bugsnag.getClient()
        val thread = HandlerThread("HandlerThread")
        thread.start()

        Handler(thread.looper).post {
            // send 1st exception
            client.startSession()
            # TODO: Fix this to not require a sleep at a later date
            Thread.sleep(50)
            client.notifyBlocking(generateException())

            // send 2nd exception after resuming a session
            client.stopSession()
            client.resumeSession()
            client.notifyBlocking(generateException())
        }
    }
}
