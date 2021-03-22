package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration

/**
 * Sends an NDK error to Bugsnag shortly after the launchDurationMillis has past.
 */
internal class CXXDelayedErrorScenario(
    config: Configuration,
    context: Context,
    eventMetadata: String?
) : Scenario(config, context, eventMetadata) {

    companion object {
        private const val CRASH_DELAY_MS = 250L
    }

    external fun crash()

    init {
        config.autoTrackSessions = false
        config.launchDurationMillis = CRASH_DELAY_MS
        System.loadLibrary("cxx-scenarios")
    }

    override fun startScenario() {
        super.startScenario()
        Bugsnag.notify(RuntimeException("first error"))
        val handler = Handler(Looper.getMainLooper())

        handler.postDelayed(
            {
                crash()
            },
            CRASH_DELAY_MS * 2
        )
    }
}
