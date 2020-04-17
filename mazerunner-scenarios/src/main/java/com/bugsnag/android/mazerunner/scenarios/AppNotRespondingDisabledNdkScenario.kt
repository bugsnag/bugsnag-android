package com.bugsnag.android.mazerunner.scenarios

import com.bugsnag.android.Configuration

import android.content.Context
import android.os.Looper
import android.os.Handler

/**
 * Stops the app from responding for a time period
 */
internal class AppNotRespondingDisabledNdkScenario(config: Configuration,
                                                   context: Context) : Scenario(config, context) {
    init {
        config.autoTrackSessions = false
        config.enabledErrorTypes.anrs = true
        config.enabledErrorTypes.ndkCrashes = false
    }

    override fun run() {
        super.run()
        val main = Handler(Looper.getMainLooper())
        main.postDelayed(Runnable {
            Thread.sleep(50000) // FOREVER
        }, 1) // A moment of delay so there is something to 'tap' onscreen
    }
}
