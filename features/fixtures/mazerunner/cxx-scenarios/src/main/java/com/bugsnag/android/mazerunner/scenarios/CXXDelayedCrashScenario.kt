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
        log("App sent to background, triggering crash in 1 seconds.")
        Handler().postDelayed({
            activate(405)
        }, 1000)
    }
}
