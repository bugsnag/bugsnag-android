package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration
import com.bugsnag.android.createDefaultErrorClient

/**
 * Sends an unhandled exception which is cached on disk to Bugsnag, then sent on a separate launch,
 * using a custom API client which modifies the request.
 */
internal class CustomClientErrorFlushScenario(config: Configuration,
                                              context: Context) : Scenario(config, context) {

    override fun run() {
        super.run()

        if ("DeliverReports" == eventMetaData) {
            Bugsnag.setErrorReportApiClient { urlString, report, headers ->
                headers["Custom-Client"] = "Hello World"
                createDefaultErrorClient(context).postReport(urlString, report, headers)
            }
        } else {
            disableAllDelivery()
            throw RuntimeException("ReportCacheScenario")
        }

    }

}
