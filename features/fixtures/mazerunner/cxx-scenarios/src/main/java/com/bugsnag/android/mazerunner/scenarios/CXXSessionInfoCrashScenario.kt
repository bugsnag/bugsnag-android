package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration
import com.bugsnag.android.createDefaultDelivery
import com.bugsnag.android.mazerunner.InterceptingDelivery
import java.util.concurrent.atomic.AtomicInteger

class CXXSessionInfoCrashScenario(
    config: Configuration,
    context: Context,
    eventMetadata: String?
) : Scenario(config, context, eventMetadata) {

    private val deliveryCount = AtomicInteger(0)

    init {
        System.loadLibrary("cxx-scenarios")

        config.delivery = InterceptingDelivery(createDefaultDelivery()) {
            when (deliveryCount.incrementAndGet()) {
                1 -> Bugsnag.notify(Exception("For the first"))
                2 -> Bugsnag.notify(Exception("For the second"))
                3 -> crash(3837)
            }
        }
    }

    external fun crash(value: Int): Int

    override fun startScenario() {
        super.startScenario()
        Bugsnag.startSession()
    }
}
