package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Client
import com.bugsnag.android.Configuration
import com.bugsnag.android.disableAllDelivery

/**
 * Configures two Bugsnag clients with different endpoints. Only the first error should be
 * reported.
 */
internal class MultiClientEndpointScenario(config: Configuration,
                                           context: Context) : Scenario(config, context) {
    var firstClient: Client? = null
    var secondClient: Client? = null

    fun configureClients() {
        firstClient = Client(context, config)

        Thread.sleep(10) // enforce request order
        val secondConfig = Configuration("abc123")
        secondConfig.endpoint = "http://localhost:1234"
        secondConfig.sessionEndpoint = "http://localhost:1234"
        secondClient = Client(context, secondConfig)
    }

    override fun run() {
        configureClients()

        if ("DeliverReport" != eventMetaData) {
            disableAllDelivery(firstClient!!)
            disableAllDelivery(secondClient!!)
            throw IllegalArgumentException("MultiClientApiKeyScenario")
        }
    }

}
