package com.bugsnag.android.mazerunner.scenarios

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration

/**
 * Sends a handled exception to Bugsnag, which includes automatic context.
 */
internal class AutoContextScenario(
    config: Configuration,
    context: Context,
    eventMetadata: String
) : Scenario(config, context, eventMetadata) {

    init {
        config.autoTrackSessions = false
    }

    override fun startScenario() {
        super.startScenario()
        registerActivityLifecycleCallbacks()
        context.startActivity(Intent("com.bugsnag.android.mazerunner.UPDATE_CONTEXT"))
    }

    override fun onActivityStarted(activity: Activity) {
        Bugsnag.notify(generateException())
    }
}
