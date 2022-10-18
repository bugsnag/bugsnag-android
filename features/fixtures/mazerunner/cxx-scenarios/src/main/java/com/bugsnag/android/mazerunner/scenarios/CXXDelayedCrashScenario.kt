package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import android.os.Handler
import android.os.Looper
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

        Handler(Looper.getMainLooper()).post {
            onAppBackgrounded {
                log("App sent to background, triggering crash.")
                activate(405)
            }
        }
    }
}
