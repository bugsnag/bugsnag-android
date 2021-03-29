package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.Configuration

/**
 * Sends an unhandled exception to Bugsnag, when another exception handler is installed.
 */
internal class CrashHandlerScenario(
    config: Configuration,
    context: Context,
    eventMetadata: String
) : Scenario(config, context, eventMetadata) {

    override fun startScenario() {
        super.startScenario()
        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            config.logger?.d("CrashHandlerScenario: Intercepted uncaught exception")
            previousHandler.uncaughtException(t, e)
        }
        throw RuntimeException("CrashHandlerScenario")
    }

    override fun getInterceptedLogMessages() =
        listOf("CrashHandlerScenario: Intercepted uncaught exception")
}
