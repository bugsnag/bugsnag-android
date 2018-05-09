package com.bugsnag.android.mazerunner.scenarios

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration
import com.bugsnag.android.mazerunner.SecondActivity

/**
 * Sends a handled exception to Bugsnag, which includes automatic context.
 */
internal class AutoContextScenario(config: Configuration,
                                   context: Context) : Scenario(config, context) {

    override fun run() {
        config.setAutoCaptureSessions(false)
        super.run()
        context.startActivity(Intent(context, SecondActivity::class.java))

        val a = context as Activity
        a.window.decorView.postDelayed({
            Bugsnag.notify(generateException())
        }, 2000)
    }

}
