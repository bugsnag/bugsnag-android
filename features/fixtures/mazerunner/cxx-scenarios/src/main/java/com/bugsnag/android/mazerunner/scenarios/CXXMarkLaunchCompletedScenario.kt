package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration
import com.bugsnag.android.createDefaultDelivery
import com.bugsnag.android.mazerunner.InterceptingDelivery
import java.util.concurrent.atomic.AtomicInteger

/**
 * Sends an NDK error to Bugsnag after markLaunchCompleted() is invoked.
 */
internal class CXXMarkLaunchCompletedScenario(
    config: Configuration,
    context: Context,
    eventMetadata: String?
) : Scenario(config, context, eventMetadata) {

    private val deliveryCount = AtomicInteger(0)

    external fun crash()

    init {
        config.launchDurationMillis = 0
        System.loadLibrary("cxx-scenarios")

        // wait for Bugsnag.notify() to complete before triggering NDK crash
        config.delivery = InterceptingDelivery(createDefaultDelivery()) {
            if (deliveryCount.incrementAndGet() == 1) {
                crash()
            }
        }
    }

    override fun startScenario() {
        super.startScenario()
        Bugsnag.notify(RuntimeException("first error"))
        Bugsnag.markLaunchCompleted()
    }
}
