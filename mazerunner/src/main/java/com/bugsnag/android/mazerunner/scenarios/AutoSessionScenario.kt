package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import android.content.Intent
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration
import com.bugsnag.android.flushAllSessions
import com.bugsnag.android.mazerunner.SecondActivity

/**
 * Sends a manual session payload to Bugsnag.
 */
internal class AutoSessionScenario(config: Configuration,
                                   context: Context) : Scenario(config, context) {

    override fun run() {
        super.run()
        Bugsnag.setUser("123", "user@example.com", "Joe Bloggs")
        context.startActivity(Intent(context, SecondActivity::class.java))
        flushAllSessions()
    }

}
