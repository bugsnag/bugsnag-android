package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import android.util.Log
import com.bugsnag.android.Configuration

/**
 * Sends an unhandled exception to Bugsnag, when another exception handler is installed.
 */
internal class CrashHandlerScenario(config: Configuration,
                                    context: Context) : Scenario(config, context) {

    override fun run() {
        super.run()
        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler({ t, e ->
            Log.d("Bugsnag", "Intercepted uncaught exception")
            previousHandler.uncaughtException(t, e)
        })
        throw RuntimeException("CrashHandlerScenario")
    }

}

