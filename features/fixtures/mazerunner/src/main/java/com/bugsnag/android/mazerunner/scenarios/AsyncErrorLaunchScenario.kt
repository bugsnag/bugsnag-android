package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.*

/**
 * Tests that only 1 request is sent in the case where stored reports are concurrently flushed,
 * in the case that a connectivity change occurs after launch.
 */
internal class AsyncErrorLaunchScenario(
    config: Configuration,
    context: Context,
    eventMetadata: String
) : Scenario(config, context, eventMetadata) {

    init {
        config.delivery = createSlowDelivery()
        config.autoTrackSessions = false
    }

    override fun startScenario() {
        super.startScenario()

        val event = generateEvent(Bugsnag.getClient())
        event.context = "AsyncErrorLaunchScenario"
        writeErrorToStore(Bugsnag.getClient(), event)
        flushErrorStoreOnLaunch(Bugsnag.getClient())
        flushErrorStoreAsync(Bugsnag.getClient())
    }

}
