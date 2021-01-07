package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.bugsnag.android.Configuration

/**
 * Stops the app from responding for a time period with ANR detection disabled
 */
internal class AppNotRespondingDisabledScenario(
    config: Configuration,
    context: Context,
    eventMetadata: String
) : Scenario(config, context, eventMetadata) {

    init {
        config.autoTrackSessions = false
        config.enabledErrorTypes.anrs = false
    }

    override fun startScenario() {
        super.startScenario()
        val main = Handler(Looper.getMainLooper())
        main.postDelayed(
            Runnable {
                Thread.sleep(50000) // FOREVER
            },
            1
        ) // A moment of delay so there is something to 'tap' onscreen
    }
}
