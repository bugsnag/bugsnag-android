package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Client
import com.bugsnag.android.Configuration
import com.bugsnag.android.disableAllDelivery

/**
 * Configures two Bugsnag clients with different API keys, and sends a handled error from one.
 */
internal class MultiClientNotifyScenario(config: Configuration,
                                         context: Context) : Scenario(config, context) {
    var firstClient: Client? = null
    var secondClient: Client? = null

    fun configureClients() {
        firstClient = Client(context, config)

        Thread.sleep(10) // enforce request order
        val secondConfig = Configuration("abc123")
        secondConfig.endpoint = config.endpoint
        secondConfig.sessionEndpoint = config.sessionEndpoint
        secondClient = Client(context, secondConfig)
    }

    override fun run() {
        configureClients()

        if ("DeliverReport" != eventMetaData) {
            disableAllDelivery(firstClient!!)
            disableAllDelivery(secondClient!!)
            secondClient!!.notify(RuntimeException("Whoops"))
        }
    }

}
