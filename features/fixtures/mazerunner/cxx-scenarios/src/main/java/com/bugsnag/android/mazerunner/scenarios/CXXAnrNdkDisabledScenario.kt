package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.Configuration

/**
 * Stops the app from responding for a time period
 */
internal class CXXAnrNdkDisabledScenario(config: Configuration,
                                         context: Context) : Scenario(config, context) {

    companion object {
        init {
            System.loadLibrary("cxx-scenarios")
        }
    }

    init {
        config.autoTrackSessions = false
        config.enabledErrorTypes.anrs = true
        config.enabledErrorTypes.ndkCrashes = false
    }

    external fun crash()

    override fun run() {
        super.run()
        crash()
    }
}
