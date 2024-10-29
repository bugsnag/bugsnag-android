package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration
import com.bugsnag.android.createDefaultDelivery
import com.bugsnag.android.mazerunner.InterceptingDelivery
import java.util.concurrent.CountDownLatch
import kotlin.concurrent.thread

/**
 * Sends an exception after pausing the session
 */
internal class ManualSessionSmokeScenario(
    config: Configuration,
    context: Context,
    eventMetadata: String
) : Scenario(config, context, eventMetadata) {

    private val deliveryLatch = CountDownLatch(3)

    init {
        val baseDelivery = createDefaultDelivery()
        config.delivery = InterceptingDelivery(baseDelivery) {
            deliveryLatch.countDown()
        }
    }

    override fun startScenario() {
        super.startScenario()
        thread {
            Bugsnag.setUser("123", "ABC.CBA.CA", "ManualSessionSmokeScenario")

            // send session
            Bugsnag.startSession()

            // send exception with session
            Bugsnag.notify(generateException())

            // send exception without session
            Bugsnag.pauseSession()
            Bugsnag.notify(generateException())

            // override to ensure request order, as the order of fatal errors
            // can be indeterminate if they are persisted to disk at the same
            // millisecond as another error.
            deliveryLatch.await()
            Bugsnag.resumeSession()
            throw generateException()
        }
    }
}
