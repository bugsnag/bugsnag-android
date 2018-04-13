package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Client
import com.bugsnag.android.Configuration
import com.bugsnag.android.disableAllDelivery

/**
 * Configures two Bugsnag clients with different API keys. The error should be captured by each,
 * and the correct API key should be used for both.
 */
internal class MultiClientApiKeyScenario(config: Configuration,
                                         context: Context) : MultiClientScenario(config, context) {

    override fun run() {
        super.run()
        disableAllDelivery(firstClient!!)
        disableAllDelivery(secondClient!!)
        throw IllegalArgumentException("MultiClientApiKeyScenario")
    }

}
