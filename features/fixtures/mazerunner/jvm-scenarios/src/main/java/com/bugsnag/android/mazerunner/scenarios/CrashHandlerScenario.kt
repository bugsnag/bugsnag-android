package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.Bugsnag
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
        val previousHandler = requireNotNull(Thread.getDefaultUncaughtExceptionHandler())
        var customHandlerInvoked = false

        // detect whether custom handler was invoked or not & add this to error reports
        Bugsnag.addOnError {
            it.addMetadata("customHandler", "invoked", customHandlerInvoked)
            true
        }

        // set a custom handler that calls into the default handler
        Thread.setDefaultUncaughtExceptionHandler { t, e ->
            customHandlerInvoked = true
            previousHandler.uncaughtException(t, e)
        }
        throw RuntimeException("CrashHandlerScenario")
    }
}
