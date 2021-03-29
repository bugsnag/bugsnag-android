package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration

/**
 * Stops the app from responding for a time period
 */
internal class JvmAnrSleepScenario(
    config: Configuration,
    context: Context,
    eventMetadata: String?
) : Scenario(config, context, eventMetadata) {

    init {
        config.enabledErrorTypes.anrs = true
    }

    override fun startScenario() {
        super.startScenario()
        // Note: Whilst other ANR scenarios use a deadlock to generate the ANR, this scenario is specifically designed
        // to test how we deal with stack traces of syscall-generating Java methods like Thread.sleep().
        Bugsnag.addMetadata("custom", "global", "present in global metadata")
        Bugsnag.addOnError { event ->
            event.addMetadata("custom", "local", "present in local metadata")
            true
        }
        val main = Handler(Looper.getMainLooper())
        main.postDelayed(
            Runnable {
                Thread.sleep(100000)
            },
            1
        ) // A moment of delay so there is something to 'tap' onscreen
    }
}
