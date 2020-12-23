package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration
import com.bugsnag.android.createSlowDelivery
import com.bugsnag.android.flushErrorStoreAsync
import com.bugsnag.android.generateEvent
import com.bugsnag.android.writeErrorToStore

/**
 * Tests that only 1 request is sent in the case where stored reports are concurrently flushed,
 * in the case that two connectivity changes occur in quick succession.
 */
internal class AsyncErrorDoubleFlushScenario(config: Configuration,
                                             context: Context) : Scenario(config, context) {

    init {
        config.delivery = createSlowDelivery()
        config.autoTrackSessions = false
    }
    override fun run() {
        super.run()

        val event = generateEvent(Bugsnag.getClient())
        event.context = "AsyncErrorDoubleFlushScenario"
        writeErrorToStore(Bugsnag.getClient(), event)
        flushErrorStoreAsync(Bugsnag.getClient())
        flushErrorStoreAsync(Bugsnag.getClient())
    }

}
