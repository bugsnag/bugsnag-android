package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.BreadcrumbType
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration

class MetadataStringsTooLargeScenario(
    config: Configuration,
    context: Context,
    eventMetadata: String
) : Scenario(config, context, eventMetadata) {

    val metadataTestValue: String

    init {
        // Args: maxStringValue (int), testStringValue (String)
        val args = eventMetadata.split(",")
        if (args.isNotEmpty()) {
            config.maxStringValueLength = args[0].toInt()
            metadataTestValue = args[1]
        } else {
            metadataTestValue = ""
        }
    }

    override fun startScenario() {
        super.startScenario()
        Bugsnag.addMetadata("custom", "foo", metadataTestValue)
        Bugsnag.leaveBreadcrumb("test", mutableMapOf<String, Any>("a" to metadataTestValue), BreadcrumbType.MANUAL)
        Bugsnag.notify(generateException())
    }
}
