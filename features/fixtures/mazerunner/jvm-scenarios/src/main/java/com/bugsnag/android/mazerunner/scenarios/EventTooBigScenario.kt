package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration

class EventTooBigScenario(
    config: Configuration,
    context: Context,
    eventMetadata: String
) : Scenario(config, context, eventMetadata) {

    val count: Int

    init {
        config.maxStringValueLength = 20000
        count = if (eventMetadata.isEmpty()) 0 else eventMetadata.toInt()
    }

    override fun startScenario() {
        super.startScenario()

        repeat(count) {
            Bugsnag.leaveBreadcrumb("${it}" + "*".repeat(10000))
        }
        Bugsnag.notify(generateException())
    }
}
