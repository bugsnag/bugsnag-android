package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.Configuration
import com.bugsnag.android.mazerunner.BugsnagIntentParams

/**
 * Send a handled C error to Bugsnag from two different processes
 */
internal class MultiProcessHandledCXXErrorScenario(
    config: Configuration,
    context: Context,
    eventMetadata: String
) : Scenario(config, context, eventMetadata) {

    init {
        config.persistUser = true
        System.loadLibrary("bugsnag-ndk")
        System.loadLibrary("cxx-scenarios-bugsnag")
    }

    external fun activate()

    override fun startScenario() {
        super.startScenario()
        if (!isRunningFromBackgroundService()) {
            launchMultiProcessService(
                BugsnagIntentParams.fromConfig(
                    config,
                    javaClass.simpleName,
                    eventMetadata
                )
            ) {
                activate()
            }
        } else {
            activate()
        }
    }
}
