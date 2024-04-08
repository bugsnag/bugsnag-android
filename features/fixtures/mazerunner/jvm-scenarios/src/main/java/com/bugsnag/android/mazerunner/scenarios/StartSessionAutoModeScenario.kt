package com.bugsnag.android.mazerunner.scenarios

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration
import com.bugsnag.android.createDefaultDelivery
import com.bugsnag.android.mazerunner.InterceptingDelivery

/**
 * Sends an automated session payload to Bugsnag.
 */
internal class StartSessionAutoModeScenario(
    config: Configuration,
    context: Context,
    eventMetadata: String?
) : Scenario(config, context, eventMetadata) {

    init {
        val baseDelivery = createDefaultDelivery()
        var intercept = true
        config.autoTrackSessions = true
        config.delivery = InterceptingDelivery(baseDelivery) {
            if (intercept) {
                intercept = false
                continueScenario()
            }
        }

    }

    private fun continueScenario() {
        Bugsnag.startSession()
        registerActivityLifecycleCallbacks()
        context.startActivity(Intent("com.bugsnag.android.mazerunner.UPDATE_CONTEXT"))
    }
}
