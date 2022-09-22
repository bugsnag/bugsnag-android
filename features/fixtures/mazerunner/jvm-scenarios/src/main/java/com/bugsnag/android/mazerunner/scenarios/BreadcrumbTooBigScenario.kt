package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration

class BreadcrumbTooBigScenario(
    config: Configuration,
    context: Context,
    eventMetadata: String
) : Scenario(config, context, eventMetadata) {

    init {
        config.maxStringValueLength = 2000000
    }

    override fun startScenario() {
        super.startScenario()

        Bugsnag.leaveBreadcrumb("test" + "*".repeat(1100000))
        Bugsnag.notify(generateException())
    }
}
