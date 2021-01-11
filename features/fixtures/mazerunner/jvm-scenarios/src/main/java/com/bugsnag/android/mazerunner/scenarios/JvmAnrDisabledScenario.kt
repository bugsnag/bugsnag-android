package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration
import java.util.Timer
import kotlin.concurrent.schedule

/**
 * Stops the app from responding for a time period with ANR detection disabled
 */
internal class JvmAnrDisabledScenario(
    config: Configuration,
    context: Context,
    eventMetadata: String?
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
                while (true) { }
            },
            1
        ) // A moment of delay so there is something to 'tap' onscreen

        // Generate a handled event after 2 seconds as a sanity check that the process didn't crash.
        Timer("HandledException", false).schedule(2000) {
            Bugsnag.notify(generateException())
        }
    }
}
