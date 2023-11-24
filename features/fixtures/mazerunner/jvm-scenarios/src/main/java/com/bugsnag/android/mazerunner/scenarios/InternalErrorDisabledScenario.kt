package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration
import com.bugsnag.android.JavaHooks.triggerInternalBugsnagForError

internal class InternalErrorDisabledScenario(
    config: Configuration,
    context: Context,
    eventMetadata: String
) : Scenario(config, context, eventMetadata) {

    init {
        config.telemetry = setOf()
    }

    override fun startScenario() {
        super.startScenario()
        triggerInternalBugsnagForError(Bugsnag.getClient())
    }
}
