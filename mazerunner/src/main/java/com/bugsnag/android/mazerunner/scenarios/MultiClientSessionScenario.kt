package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.*

/**
 * Configures two Bugsnag clients with different API keys. Two sessions are manually started
 * for each client - the correct API key and user name should be used for both.
 */
internal class MultiClientSessionScenario(config: Configuration,
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

        firstClient!!.setUserName("Bob")
        secondClient!!.setUserName("Alice")
    }

    override fun run() {
        configureClients()

        if ("DeliverSessions" == eventMetaData) {
            flushAllSessions(firstClient!!)
            Thread.sleep(10) // enforce request order
            flushAllSessions(secondClient!!)
        } else {
            disableAllDelivery(firstClient!!)
            disableAllDelivery(secondClient!!)
            firstClient!!.startSession()
            secondClient!!.startSession()
        }
    }

}
