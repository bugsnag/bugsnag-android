package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration
import com.bugsnag.android.createDefaultDelivery
import com.bugsnag.android.mazerunner.InterceptingDelivery
import java.util.concurrent.atomic.AtomicInteger

/**
 * Sends an exception after pausing the session
 */
internal class ManualSessionSmokeScenario(
    config: Configuration,
    context: Context,
    eventMetadata: String
) : Scenario(config, context, eventMetadata) {

    init {
        config.autoTrackSessions = false

        if (eventMetadata != "non-crashy") {
            val baseDelivery = createDefaultDelivery()
            val state = AtomicInteger(0)
            config.delivery = InterceptingDelivery(baseDelivery) {
                when (state.incrementAndGet()) {
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
            }
        }
    }

    override fun startScenario() {
        super.startScenario()
        if (eventMetadata != "non-crashy") {
            Bugsnag.setUser("123", "ABC.CBA.CA", "ManualSessionSmokeScenario")
            Bugsnag.startSession()
        }
    }
}
