package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.Client
import com.bugsnag.android.Configuration
import com.bugsnag.android.disableAllDelivery
import com.bugsnag.android.writeErrorToOldDir

/**
 * Configures two Bugsnag clients with different API keys. A single error report is written to the
 * old directory format, which should then be reported by both clients.
 *
 */
internal class MultiClientOldDirScenario(config: Configuration,
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
            writeErrorToOldDir(firstClient!!)
        }
    }

}
