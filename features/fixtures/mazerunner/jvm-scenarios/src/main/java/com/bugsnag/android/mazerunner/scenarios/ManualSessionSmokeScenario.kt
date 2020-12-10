package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.*
import com.bugsnag.android.JavaHooks.generateDelivery
import com.bugsnag.android.InterceptingDelivery

/**
 * Sends an exception after pausing the session
 */
internal class ManualSessionSmokeScenario(config: Configuration,
                                          context: Context) : Scenario(config, context) {

    init {
        config.autoTrackSessions = false
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

    override fun run() {
        super.run()
        Bugsnag.setUser("123", "ABC.CBA.CA", "ManualSessionSmokeScenario")
        Bugsnag.startSession()
    }
}
