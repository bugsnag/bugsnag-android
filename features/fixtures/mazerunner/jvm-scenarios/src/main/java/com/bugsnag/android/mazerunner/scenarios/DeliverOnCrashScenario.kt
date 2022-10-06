package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.Configuration

private const val TIMEOUT = 15_000L

internal class DeliverOnCrashScenario(
    config: Configuration,
    context: Context,
    eventMetadata: String
) : Scenario(config, context, eventMetadata) {

    init {
        config.isAttemptDeliveryOnCrash = true
        val deliveryDelegate = Class.forName("com.bugsnag.android.DeliveryDelegate")
        deliveryDelegate.getDeclaredField("DELIVERY_TIMEOUT").apply {
            isAccessible = true
            set(null, TIMEOUT)
        }
    }

    override fun startScenario() {
        super.startScenario()
        throw RuntimeException("DeliverOnCrashScenario")
    }
}
