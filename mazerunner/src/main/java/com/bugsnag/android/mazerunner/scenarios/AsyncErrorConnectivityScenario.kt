package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.*

/**
 * Tests that only 1 request is sent in the case where stored reports are concurrently flushed,
 * in the case that a connectivity change occurs before launch.
 */
internal class AsyncErrorConnectivityScenario(config: Configuration,
                                              context: Context) : Scenario(config, context) {

    override fun run() {
        val delivery = createSlowDelivery(context)
        config.delivery = delivery
        super.run()

        writeErrorToStore(Bugsnag.getClient())
        flushErrorStoreAsync(Bugsnag.getClient())
        flushErrorStoreOnLaunch(Bugsnag.getClient())
        Thread.sleep(50)
    }

}
