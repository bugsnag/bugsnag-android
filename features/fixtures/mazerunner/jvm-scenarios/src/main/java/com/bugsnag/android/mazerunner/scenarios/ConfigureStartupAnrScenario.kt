package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.Configuration
import kotlin.system.exitProcess

class ConfigureStartupAnrScenario(
    config: Configuration,
    context: Context,
    eventMetadata: String
) : Scenario(config, context, eventMetadata) {
    override fun startScenario() {
        context.applicationContext
            .getSharedPreferences("AnrPreferences", Context.MODE_PRIVATE)
            .edit()
            .putLong("onCreateDelay", STARTUP_DELAY)
            .commit()

        exitProcess(0)
    }

    companion object {
        private const val STARTUP_DELAY = 430L
    }
}
