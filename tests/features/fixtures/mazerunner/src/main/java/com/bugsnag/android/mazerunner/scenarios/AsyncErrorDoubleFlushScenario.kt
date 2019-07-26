package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.*

/**
 * Tests that only 1 request is sent in the case where stored reports are concurrently flushed,
 * in the case that two connectivity changes occur in quick succession.
 */
internal class AsyncErrorDoubleFlushScenario(config: Configuration,
                                             context: Context) : Scenario(config, context) {

    init {
        config.delivery = createSlowDelivery(config)
    }

    override fun run() {
        super.run()

        writeErrorToStore(Bugsnag.getClient())
        flushErrorStoreAsync(Bugsnag.getClient())
        flushErrorStoreAsync(Bugsnag.getClient())
    }

}
