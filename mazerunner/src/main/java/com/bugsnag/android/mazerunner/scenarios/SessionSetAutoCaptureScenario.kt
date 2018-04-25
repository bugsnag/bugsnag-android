package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import android.content.Intent
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration
import com.bugsnag.android.mazerunner.SecondActivity

/**
 * Sets automatic capture of sessions in Bugsnag and flushes 1 session
 */
internal class SessionSetAutoCaptureScenario(config: Configuration,
                                             context: Context) : Scenario(config, context) {

    override fun run() {
        super.run()
        Bugsnag.setAutoCaptureSessions(true)
        context.startActivity(Intent(context, SecondActivity::class.java))
    }

}
