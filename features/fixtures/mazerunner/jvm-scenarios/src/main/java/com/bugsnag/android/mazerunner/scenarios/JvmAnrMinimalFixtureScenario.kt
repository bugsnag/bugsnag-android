package com.bugsnag.android.mazerunner.scenarios

import com.bugsnag.android.Configuration

import android.content.Context
import android.os.Looper
import android.os.Handler
import com.bugsnag.android.Bugsnag

/**
 * Stops the app from responding for a time period
 */
internal class JvmAnrMinimalFixtureScenario(config: Configuration,
                                            context: Context) : Scenario(config, context) {
    init {
        config.autoTrackSessions = false
        config.enabledErrorTypes.anrs = true
    }

    override fun run() {
        super.run()
        Bugsnag.notify(generateException())
        val main = Handler(Looper.getMainLooper())
        main.postDelayed(Runnable {
            // choose Thread.sleep as the method is native and will count as a
            // native ANR. this scenario validates that native ANRs are captured
            // even if the NDK plugin is completely excluded, and that bugsnag captures
            // only a JVM stacktrace.
            Thread.sleep(50000)
        }, 1) // A moment of delay so there is something to 'tap' onscreen
    }
}
