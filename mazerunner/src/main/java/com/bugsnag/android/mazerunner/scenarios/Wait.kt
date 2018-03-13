package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import android.os.Handler
import android.os.HandlerThread
import com.bugsnag.android.Configuration
import com.bugsnag.android.flushAllSessions

internal class Wait(config: Configuration, context: Context) : Scenario(config, context) {

    override fun run() {
        super.run()
        val thread = HandlerThread("HandlerThread")
        thread.start()
        Handler(thread.looper).post {
            flushAllSessions()
        }
    }
}
