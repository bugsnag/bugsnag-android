package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration
import java.io.File

internal class EmptyReportScenario(
    config: Configuration,
    context: Context,
    eventMetadata: String
) : Scenario(config, context, eventMetadata) {

    override fun startBugsnag(startBugsnagOnly: Boolean) {
        if (startBugsnagOnly) {
            disableAllDelivery(config)
        }
    }

    override fun startScenario() {
        super.startScenario()

        Bugsnag.notify(java.lang.RuntimeException("Whoops"))
    }
}
