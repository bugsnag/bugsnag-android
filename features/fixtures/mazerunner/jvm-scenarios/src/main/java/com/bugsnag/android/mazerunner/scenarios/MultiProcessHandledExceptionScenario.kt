package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration
import com.bugsnag.android.mazerunner.BugsnagIntentParams

/**
 * Sends handled exceptions to Bugsnag from two different processes
 */
internal class MultiProcessHandledExceptionScenario(
    config: Configuration,
    context: Context,
    eventMetadata: String
) : Scenario(config, context, eventMetadata) {

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
                Bugsnag.setUser("2", "background@example.com", "MultiProcessHandledExceptionScenario")
                Bugsnag.notify(generateException())
            }
        } else {
            Bugsnag.setUser("1", "foreground@example.com", "MultiProcessHandledExceptionScenario")
            Bugsnag.notify(generateException())
        }
    }
}
