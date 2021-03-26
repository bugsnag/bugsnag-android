package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.Configuration
import com.bugsnag.android.mazerunner.getZeroEventsLogMessages

class CXXThrowSomethingOutsideReleaseStagesScenario(
    config: Configuration,
    context: Context,
    eventMetadata: String?
) : Scenario(config, context, eventMetadata) {

    /**
     * Sets custom enabled release stages.
     */
    init {
        config.enabledReleaseStages = setOf("fee-fi-fo-fum")
        System.loadLibrary("cxx-scenarios")
    }

    external fun crash(num: Int)

    override fun startScenario() {
        super.startScenario()
        crash(23)
    }

    override fun getInterceptedLogMessages(): List<String> {
        return getZeroEventsLogMessages(startBugsnagOnly)
    }
}
