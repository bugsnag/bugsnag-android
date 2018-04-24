package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration
import com.bugsnag.android.createDefaultErrorClient

/**
 * Sends a handled exception to Bugsnag using a custom API client which modifies the request.
 */
internal class CustomClientErrorScenario(config: Configuration,
                                         context: Context) : Scenario(config, context) {

    override fun run() {
        super.run()

        Bugsnag.setErrorReportApiClient { urlString, report, headers ->
            headers["Custom-Client"] = "Hello World"
            createDefaultErrorClient(context).postReport(urlString, report, headers)
        }
        Bugsnag.notify(RuntimeException("Hello"))
    }

}
