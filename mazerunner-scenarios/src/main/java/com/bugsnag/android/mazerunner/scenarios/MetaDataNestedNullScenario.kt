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

    init {
        config.setAutoCaptureSessions(false)

    }

    override fun run() {
        super.run()

        val configMap = HashMap<String, Any?>()
        configMap["test"] = null
        Bugsnag.addMetadata("Custom", "foo", configMap)

        Bugsnag.notify(RuntimeException("MetaDataScenario")) {
            val map = HashMap<String, Any?>()
            map["test"] = null
            it.error.addMetadata("Custom", "foo", map)
        }
    }

}
