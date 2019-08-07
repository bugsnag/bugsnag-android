package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.*

/**
 * Tests that only 1 request is sent in the case where stored reports are concurrently flushed,
 * in the case that a connectivity change occurs before launch.
 */
internal class AsyncErrorConnectivityScenario(config: Configuration,
                                              context: Context) : Scenario(config, context) {

    init {
        val delivery = createSlowDelivery(config)
        config.delivery = delivery
        config.setAutoCaptureSessions(false)
    }

    override fun run() {
        super.run()

        writeErrorToStore(Bugsnag.getClient())
        flushErrorStoreAsync(Bugsnag.getClient())
        flushErrorStoreOnLaunch(Bugsnag.getClient())
    }

}
