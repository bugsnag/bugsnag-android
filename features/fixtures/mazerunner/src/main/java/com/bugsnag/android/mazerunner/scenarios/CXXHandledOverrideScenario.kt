package com.bugsnag.android.mazerunner.scenarios

import android.content.Context

import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration
import com.bugsnag.android.OnSessionCallback

internal class CXXHandledOverrideScenario(
    config: Configuration,
    context: Context
) : Scenario(config, context) {

    init {
        System.loadLibrary("entrypoint")
        config.autoTrackSessions = false
        disableSessionDelivery(config)
    }

    external fun activate()

    override fun run() {
        super.run()

        if (eventMetaData != "non-crashy") {
            Bugsnag.startSession()
            activate()
        }
    }
}
