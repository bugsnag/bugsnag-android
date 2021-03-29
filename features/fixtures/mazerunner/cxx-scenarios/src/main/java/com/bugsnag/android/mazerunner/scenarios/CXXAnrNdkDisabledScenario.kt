package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.Configuration

/**
 * Stops the app from responding for a time period
 */
internal class CXXAnrNdkDisabledScenario(
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
        config.enabledErrorTypes.anrs = true
        config.enabledErrorTypes.ndkCrashes = false
    }

    external fun crash()

    override fun startScenario() {
        super.startScenario()
        crash()
    }
}
