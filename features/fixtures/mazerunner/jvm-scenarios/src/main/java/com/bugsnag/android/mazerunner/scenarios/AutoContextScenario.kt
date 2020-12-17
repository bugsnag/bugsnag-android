package com.bugsnag.android.mazerunner.scenarios

import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration

import android.app.Activity
import android.content.Context
import android.content.Intent

/**
 * Sends a handled exception to Bugsnag, which includes automatic context.
 */
internal class AutoContextScenario(config: Configuration,
                                   context: Context) : Scenario(config, context) {
    init {
        config.autoTrackSessions = false
    }

    override fun run() {
        super.run()
        registerActivityLifecycleCallbacks()
        context.startActivity(Intent("com.bugsnag.android.mazerunner.UPDATE_CONTEXT"))
    }

    override fun onActivityStarted(activity: Activity) {
        Bugsnag.notify(generateException())
    }
}
