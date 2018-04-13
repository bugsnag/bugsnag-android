package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration
import com.bugsnag.android.disableAllDelivery

/**
 * Sends an unhandled exception which is cached on disk to Bugsnag, then sent on a separate launch.
 */
internal class ReportCacheScenario(config: Configuration,
                                   context: Context) : Scenario(config, context) {

    override fun run() {
        super.run()
        disableAllDelivery(Bugsnag.getClient())
        throw RuntimeException("ReportCacheScenario")
    }

}
