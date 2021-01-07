package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import android.util.Log
import com.bugsnag.android.Configuration

/**
 * Sends an unhandled exception to Bugsnag, when another exception handler is installed.
 */
internal class CrashHandlerScenario(
    config: Configuration,
    context: Context,
    eventMetadata: String
) : Scenario(config, context, eventMetadata) {

    init {
        config.autoTrackSessions = false
    }

    override fun startScenario() {
        super.startScenario()
        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler({ t, e ->
            Log.d("Bugsnag", "Intercepted uncaught exception")
            previousHandler.uncaughtException(t, e)
        })
        throw RuntimeException("CrashHandlerScenario")
    }
}
