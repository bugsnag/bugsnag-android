package com.bugsnag.android.mazerunner.scenarios

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle

import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration
import com.bugsnag.android.mazerunner.SecondActivity

/**
 * Sends a handled exception to Bugsnag, which includes automatic context.
 */
internal class AutoContextScenario(config: Configuration,
                                   context: Context) : Scenario(config, context) {
    override fun run() {
        super.run()
        registerActivityLifecycleCallbacks()
        context.startActivity(Intent(context, SecondActivity::class.java))
    }

    override fun onActivityStarted(activity: Activity) {
        Bugsnag.notify(generateException())
    }
}
