package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.Configuration
import com.bugsnag.android.mazerunner.BugsnagIntentParams

/**
 * Sends unhandled exceptions to Bugsnag from two different processes
 */
internal class MultiProcessUnhandledExceptionScenario(
    config: Configuration,
    context: Context,
    eventMetadata: String
) : Scenario(config, context, eventMetadata) {

    override fun startScenario() {
        super.startScenario()

        if (eventMetadata == "main-activity") {
            throw generateException()
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
                runOnBgThread {
                    throw generateException()
                }
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
