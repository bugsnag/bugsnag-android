package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration
import com.bugsnag.android.createDefaultDelivery
import com.bugsnag.android.mazerunner.InterceptingDelivery

class CXXPausedSessionScenario(
    config: Configuration,
    context: Context,
    eventMetadata: String?
) : Scenario(config, context, eventMetadata) {

    init {
        System.loadLibrary("cxx-scenarios")

        config.delivery = InterceptingDelivery(createDefaultDelivery()) {
            crash(0)
        }
    }

    external fun crash(counter: Int): Int

    override fun startScenario() {
        super.startScenario()
        Bugsnag.startSession()
        Bugsnag.pauseSession()
    }
}
