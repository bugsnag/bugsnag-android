package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration

/**
 * Attempts to send a handled exception to Bugsnag, when the exception handler is disabled,
 * which should result in no operation.
 */
internal class DisableAutoNotifyScenario(config: Configuration,
                                         context: Context) : Scenario(config, context) {

    override fun run() {
        super.run()
        Bugsnag.disableExceptionHandler()
        throw RuntimeException("Should never appear")
    }

}
