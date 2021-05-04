package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration
import com.bugsnag.android.mazerunner.getZeroEventsLogMessages
import com.bugsnag.android.setAutoNotify

class UnhandledNdkAutoNotifyFalseScenario(
    config: Configuration,
    context: Context,
    eventMetadata: String?
) : Scenario(config, context, eventMetadata) {

    init {
        System.loadLibrary("cxx-scenarios")
    }

    external fun crash()

    override fun startScenario() {
        super.startScenario()
        setAutoNotify(Bugsnag.getClient(), false)
        crash()
    }

    override fun getInterceptedLogMessages(): List<String> {
        return getZeroEventsLogMessages(startBugsnagOnly)
    }
}
