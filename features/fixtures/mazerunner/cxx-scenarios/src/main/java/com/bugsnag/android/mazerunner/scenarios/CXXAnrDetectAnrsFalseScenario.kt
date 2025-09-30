package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.Configuration
import com.bugsnag.android.mazerunner.getZeroEventsLogMessages

/**
 * Stops the app from responding for a time period
 */
internal class CXXAnrDetectAnrsFalseScenario(
    config: Configuration,
    context: Context,
    eventMetadata: String?
) : Scenario(config, context, eventMetadata) {

    companion object {
        init {
            System.loadLibrary("cxx-scenarios")
        }
    }

    init {
        config.enabledErrorTypes.anrs = false
        config.enabledErrorTypes.ndkCrashes = true
    }

    external fun crash()

    override fun startScenario() {
        super.startScenario()
        crash()
    }

    override fun getInterceptedLogMessages(): List<String> {
        return getZeroEventsLogMessages(startBugsnagOnly)
    }
}
