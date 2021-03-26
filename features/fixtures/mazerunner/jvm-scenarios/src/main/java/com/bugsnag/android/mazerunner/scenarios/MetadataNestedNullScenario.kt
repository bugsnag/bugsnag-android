package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration
import java.util.HashMap

/**
 * Sends a handled exception to Bugsnag, which includes a nested null value in a metadata map
 */
internal class MetadataNestedNullScenario(
    config: Configuration,
    context: Context,
    eventMetadata: String
) : Scenario(config, context, eventMetadata) {

    override fun startScenario() {
        super.startScenario()

        val configMap = HashMap<String, Any?>()
        configMap["test"] = null
        Bugsnag.addMetadata("Custom", "foo", configMap)

        Bugsnag.notify(RuntimeException("MetadataScenario")) {
            val map = HashMap<String, Any?>()
            map["test"] = null
            it.addMetadata("Custom", "foo", map)
            true
        }
    }
}
