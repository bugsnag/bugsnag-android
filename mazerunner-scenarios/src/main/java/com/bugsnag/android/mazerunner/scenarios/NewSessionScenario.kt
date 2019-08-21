package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration
import android.os.Handler
import android.os.HandlerThread

/**
 * Sends an exception after stopping the session
 */
internal class NewSessionScenario(config: Configuration,
                                      context: Context) : Scenario(config, context) {
    init {
        config.setAutoCaptureSessions(false)
    }

    override fun run() {
        super.run()
        val client = Bugsnag.getClient()
        val thread = HandlerThread("HandlerThread")
        thread.start()

        Handler(thread.looper).post(Runnable {
            // send 1st exception which should include session info
            client.startSession()
            client.notifyBlocking(generateException())

            // stop tracking the existing session
            client.stopSession()

            // send 2nd exception which should contain new session info
            client.startSession()
            client.notifyBlocking(generateException())
        })
    }
}
