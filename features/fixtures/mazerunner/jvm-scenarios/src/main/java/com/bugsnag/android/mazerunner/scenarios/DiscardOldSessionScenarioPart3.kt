package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.Configuration

internal class DiscardOldSessionScenarioPart3(
    config: Configuration,
    context: Context,
    eventMetadata: String
) : Scenario(config, context, eventMetadata) {

    init {
        config.launchDurationMillis = 0
        // Note: No impediments to sending.
    }
}
