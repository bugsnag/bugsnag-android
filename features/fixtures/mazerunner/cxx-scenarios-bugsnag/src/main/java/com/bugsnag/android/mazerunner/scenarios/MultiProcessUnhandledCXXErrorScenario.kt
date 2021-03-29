package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.Configuration
import com.bugsnag.android.mazerunner.BugsnagIntentParams

/**
 * Send an unhandled C error to Bugsnag from two different processes
 */
internal class MultiProcessUnhandledCXXErrorScenario(
    config: Configuration,
    context: Context,
    eventMetadata: String
) : Scenario(config, context, eventMetadata) {

    init {
        System.loadLibrary("bugsnag-ndk")
        System.loadLibrary("cxx-scenarios-bugsnag")
    }

    external fun activate()
    external fun user1()
    external fun user2()

    override fun startScenario() {
        super.startScenario()
        if (eventMetadata == "main-activity") {
            user2()
            activate()
        } else if (eventMetadata == "multi-process-service") {
            if (!isRunningFromBackgroundService()) {
                launchMultiProcessService(
                    BugsnagIntentParams.fromConfig(
                        config,
                        javaClass.simpleName,
                        eventMetadata
                    )
                )
            } else {
                user1()
                activate()
            }
        } else {
            if (!isRunningFromBackgroundService()) {
                launchMultiProcessService(
                    BugsnagIntentParams.fromConfig(
                        config,
                        null,
                        eventMetadata
                    )
                )
            }
        }
    }
}
