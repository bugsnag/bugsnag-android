package com.bugsnag.android.mazerunner.scenarios

import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration

import android.os.Looper
import android.os.Handler
import android.content.Context
import java.util.Timer
import kotlin.concurrent.schedule

/**
 * Stops the app from responding for a time period with ANR detection disabled
 */
internal class JvmAnrDisabledScenario(config: Configuration,
                                      context: Context) : Scenario(config, context) {
    init {
        config.autoTrackSessions = false
        config.enabledErrorTypes.anrs = false
    }

    override fun run() {
        super.run()
        val main = Handler(Looper.getMainLooper())
        main.postDelayed(Runnable {
            while (true) { }
        }, 1) // A moment of delay so there is something to 'tap' onscreen

        // Generate a handled event after 2 seconds as a sanity check that the process didn't crash.
        Timer("HandledException", false).schedule(2000) { 
            Bugsnag.notify(generateException())
        }
    }

}
