package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration

/**
 * Sends a JVM error to Bugsnag shortly after the launchDurationMillis has past.
 */
internal class JvmDelayedErrorScenario(
    config: Configuration,
    context: Context,
    eventMetadata: String?
) : Scenario(config, context, eventMetadata) {

    companion object {
        private const val CRASH_DELAY_MS = 2000L
        private const val NOTIFY_DELAY_MS = CRASH_DELAY_MS * 2
    }

    init {
        config.launchDurationMillis = CRASH_DELAY_MS
    }

    override fun startScenario() {
        super.startScenario()
        Bugsnag.notify(RuntimeException("first error"))

        val handler = Handler(Looper.getMainLooper())

        handler.postDelayed(
            {
                Bugsnag.notify(generateException())
            },
            NOTIFY_DELAY_MS
        )
    }
}
