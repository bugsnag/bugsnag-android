package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration
import com.bugsnag.android.createCustomHeaderDelivery

/**
 * Sends an unhandled exception which is cached on disk to Bugsnag, then sent on a separate launch,
 * using a custom API client which modifies the request.
 */
internal class CustomClientErrorFlushScenario(config: Configuration,
                                              context: Context) : Scenario(config, context) {

    override fun run() {
        if ("DeliverReports" == eventMetaData) {
            config.delivery = createCustomHeaderDelivery(context)
        }
        super.run()

        if ("DeliverReports" != eventMetaData) {
            disableAllDelivery()
            throw RuntimeException("ReportCacheScenario")
        }

    }

}
