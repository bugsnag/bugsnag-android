package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration
import java.util.HashMap

/**
 * Sends a handled exception to Bugsnag, which includes a nested null value in a metadata map
 */
internal class MetaDataNestedNullScenario(
    config: Configuration,
    context: Context
) : Scenario(config, context) {

    override fun run() {
        super.run()

        val configMap = HashMap<String, Any?>()
        configMap["test"] = null
        Bugsnag.addToTab("Custom", "foo", configMap)

        Bugsnag.notify(RuntimeException("MetaDataScenario")) {
            val map = HashMap<String, Any?>()
            map["test"] = null
            it.error?.addToTab("Custom", "foo", map)
        }
    }

}
