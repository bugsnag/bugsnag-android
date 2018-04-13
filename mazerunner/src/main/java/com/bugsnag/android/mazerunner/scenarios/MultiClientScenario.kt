package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Client
import com.bugsnag.android.Configuration

/**
 * Configures two Bugsnag clients with different API keys.
 */
internal open class MultiClientScenario(config: Configuration,
                                   context: Context) : Scenario(config, context) {

    var firstClient: Client? = null
    var secondClient: Client? = null

    override fun run() {
        firstClient = Client(context, config)

        Thread.sleep(10) // enforce request order
        val secondConfig = Configuration("abc123")
        secondConfig.endpoint = config.endpoint
        secondConfig.sessionEndpoint = config.sessionEndpoint
        secondClient = Client(context, secondConfig)
    }

}
