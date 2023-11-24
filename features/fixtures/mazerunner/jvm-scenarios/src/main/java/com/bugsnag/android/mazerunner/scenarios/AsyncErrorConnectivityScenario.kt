package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration
import com.bugsnag.android.JavaHooks.flushErrorStoreAsync
import com.bugsnag.android.JavaHooks.flushErrorStoreOnLaunch
import com.bugsnag.android.JavaHooks.writeErrorToStore
import com.bugsnag.android.createSlowDelivery
import com.bugsnag.android.generateEvent

/**
 * Tests that only 1 request is sent in the case where stored reports are concurrently flushed,
 * in the case that a connectivity change occurs before launch.
 */
internal class AsyncErrorConnectivityScenario(
    config: Configuration,
    context: Context,
    eventMetadata: String
) : Scenario(config, context, eventMetadata) {

    init {
        val delivery = createSlowDelivery()
        config.delivery = delivery
    }

    override fun startScenario() {
        super.startScenario()
        val event = generateEvent(Bugsnag.getClient())
        event.context = "AsyncErrorConnectivityScenario"
        writeErrorToStore(Bugsnag.getClient(), event)
        flushErrorStoreAsync(Bugsnag.getClient())
        flushErrorStoreOnLaunch(Bugsnag.getClient())
    }
}
