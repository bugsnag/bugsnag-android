package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration
import com.bugsnag.android.createDefaultDelivery
import com.bugsnag.android.mazerunner.InterceptingDelivery
import com.bugsnag.android.mazerunner.log

/**
 * Sends an NDK error to Bugsnag shortly after the launchDurationMillis has past.
 */
internal class CXXDelayedErrorScenario(
    config: Configuration,
    context: Context,
    eventMetadata: String?
) : Scenario(config, context, eventMetadata) {

    companion object {
        private const val CRASH_DELAY_MS = 2500L
    }

    external fun crash()

    init {
        System.loadLibrary("cxx-scenarios")
        config.launchDurationMillis = CRASH_DELAY_MS
        config.delivery = InterceptingDelivery(createDefaultDelivery()) {
            val handler = Handler(Looper.getMainLooper())

            handler.postDelayed(
                {
                    log("Trigger native crash")
                    crash()
                },
                CRASH_DELAY_MS * 2
            )
        }
    }

    override fun startScenario() {
        super.startScenario()
        log("Notify JVM error")
        Bugsnag.notify(RuntimeException("first error"))
    }
}
