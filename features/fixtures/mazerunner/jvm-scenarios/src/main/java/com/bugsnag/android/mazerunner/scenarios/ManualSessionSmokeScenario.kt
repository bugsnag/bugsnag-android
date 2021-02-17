package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration
import com.bugsnag.android.createDefaultDelivery
import com.bugsnag.android.mazerunner.InterceptingDelivery

/**
 * Sends an exception after pausing the session
 */
internal class ManualSessionSmokeScenario(
    config: Configuration,
    context: Context,
    eventMetadata: String
) : Scenario(config, context, eventMetadata) {

    override fun startBugsnag(startBugsnagOnly: Boolean) {
        config.autoTrackSessions = false

        if (!startBugsnagOnly) {
            val baseDelivery = createDefaultDelivery()
            var state = 0
            config.delivery = InterceptingDelivery(baseDelivery) {
                when (state) {
                    0 -> Bugsnag.notify(generateException())
                    1 -> {
                        Bugsnag.pauseSession()
                        Bugsnag.notify(generateException())
                    }
                    2 -> {
                        Bugsnag.resumeSession()
                        throw generateException()
                    }
                }
                state++
            }
        }
        super.startBugsnag(startBugsnagOnly)
    }

    override fun startScenario() {
        super.startScenario()
        Bugsnag.setUser("123", "ABC.CBA.CA", "ManualSessionSmokeScenario")
        Bugsnag.startSession()
    }
}
