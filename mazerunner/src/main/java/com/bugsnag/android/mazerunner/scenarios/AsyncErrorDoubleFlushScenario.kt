package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.*

/**
 * Tests that only 1 request is sent in the case where stored reports are concurrently flushed,
 * in the case that two connectivity changes occur in quick succession.
 */
internal class AsyncErrorDoubleFlushScenario(config: Configuration,
                                             context: Context) : Scenario(config, context) {

    override fun run() {
        super.run()
        val apiClient = createSlowErrorApiClient(context)
        Bugsnag.setErrorReportApiClient(apiClient)

        writeErrorToStore(Bugsnag.getClient())
        flushErrorStoreAsync(Bugsnag.getClient(), apiClient)
        flushErrorStoreAsync(Bugsnag.getClient(), apiClient)
        Thread.sleep(50)
    }

}
