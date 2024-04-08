package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.Configuration
import kotlin.system.exitProcess

internal class StartSessionAutoModeScenario(
    config: Configuration,
    context: Context,
    eventMetadata: String?
) : Scenario(config, context, eventMetadata) {
    override fun startScenario() {
        context.applicationContext
            .getSharedPreferences("SessionPreferences", Context.MODE_PRIVATE)
            .edit()
            .putBoolean("manualSession", MANUAL_START)
            .putString("notify", config.endpoints.notify)
            .putString("sessions", config.endpoints.sessions)
            .commit()

        exitProcess(0)
    }

    companion object {
        private const val MANUAL_START = true
    }
}
