package com.bugsnag.android.mazerunner.scenarios

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import com.bugsnag.android.Configuration
import java.util.concurrent.atomic.AtomicBoolean

internal class CXXBackgroundNotifyScenario(
    config: Configuration,
    context: Context,
    eventMetadata: String
) : Scenario(config, context, eventMetadata) {

    init {
        System.loadLibrary("bugsnag-ndk")
        System.loadLibrary("cxx-scenarios-bugsnag")
    }

    private var triggered = AtomicBoolean(false)

    external fun activate()

    override fun startScenario() {
        super.startScenario()
        registerActivityLifecycleCallbacks()
    }

    override fun onActivityStopped(activity: Activity) {
        Handler(Looper.getMainLooper()).post {
            // debounce so this can only ever occur once
            if (!triggered.getAndSet(true)) {
                activate()
            }
        }
    }
}
