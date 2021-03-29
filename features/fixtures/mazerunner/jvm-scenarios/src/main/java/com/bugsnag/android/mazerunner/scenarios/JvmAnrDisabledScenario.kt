package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration
import com.bugsnag.android.mazerunner.createDeadlock

/**
 * Stops the app from responding for a time period with ANR detection disabled
 */
internal class JvmAnrDisabledScenario(
    config: Configuration,
    context: Context,
    eventMetadata: String?
) : Scenario(config, context, eventMetadata) {

    init {
        config.enabledErrorTypes.anrs = false
    }

    override fun startScenario() {
        super.startScenario()
        Bugsnag.notify(generateException())
        createDeadlock()
    }
}
