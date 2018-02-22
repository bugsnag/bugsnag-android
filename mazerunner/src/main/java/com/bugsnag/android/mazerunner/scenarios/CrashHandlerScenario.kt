package com.bugsnag.android.mazerunner.scenarios

import android.util.Log

/**
 * Sends an unhandled exception to Bugsnag, when another exception handler is installed.
 */
internal class CrashHandlerScenario : Scenario() {

    override fun run() {
        val previousHandler = Thread.getDefaultUncaughtExceptionHandler()

        Thread.setDefaultUncaughtExceptionHandler({ t, e ->
            Log.d("Bugsnag", "Intercepted uncaught exception")
            previousHandler.uncaughtException(t, e)
        })
        throw RuntimeException("CrashHandlerScenario")
    }

}

