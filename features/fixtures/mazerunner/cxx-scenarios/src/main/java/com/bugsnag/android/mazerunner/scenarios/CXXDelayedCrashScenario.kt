package com.bugsnag.android.mazerunner.scenarios

import android.app.Activity
import android.content.Context
import android.os.Handler
import com.bugsnag.android.Configuration
import com.bugsnag.android.mazerunner.log

class CXXDelayedCrashScenario(
    config: Configuration,
    context: Context,
    eventMetadata: String?
) : Scenario(config, context, eventMetadata) {

    companion object {
        init {
            private const val DELAY_MS = 1000
            private const val CRASH_TRIGGER = 450
            System.loadLibrary("cxx-scenarios")
        }
    }

    external fun activate(value: Int): Int

    override fun startScenario() {
        super.startScenario()
        registerActivityLifecycleCallbacks()
    }

    override fun onActivityStopped(activity: Activity) {
        super.onActivityStopped(activity)
        log("App sent to background, triggering crash in 0.5 seconds.")
        Handler().postDelayed({
            activate(CRASH_TRIGGER)
        }, DELAY_MS)
    }
}
