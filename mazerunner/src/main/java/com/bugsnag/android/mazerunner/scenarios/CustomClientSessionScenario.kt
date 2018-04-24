package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration
import com.bugsnag.android.createDefaultSessionClient

/**
 * Sends a session using a custom API client which modifies the request.
 */
internal class CustomClientSessionScenario(config: Configuration,
                                           context: Context) : Scenario(config, context) {

    override fun run() {
        super.run()

        Bugsnag.setSessionTrackingApiClient { urlString, report, headers ->
            headers["Custom-Client"] = "Hello World"
            val sessionClient = createDefaultSessionClient(context)
            sessionClient.postSessionTrackingPayload(urlString, report, headers)
        }

        Bugsnag.startSession()
    }

}
