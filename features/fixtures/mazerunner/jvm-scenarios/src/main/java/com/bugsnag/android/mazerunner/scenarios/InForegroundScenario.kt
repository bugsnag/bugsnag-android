package com.bugsnag.android.mazerunner.scenarios

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration
import com.bugsnag.android.mazerunner.log
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Sends a handled exception to Bugsnag, which has a short delay to allow the app to remain
 * in the foreground for ~1 second
 */
internal class InForegroundScenario(
    config: Configuration,
    context: Context,
    eventMetadata: String
) : Scenario(config, context, eventMetadata) {

    private var triggered = AtomicBoolean(false)
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun startScenario() {
        super.startScenario()
        registerActivityLifecycleCallbacks()

        // make sure the app goes to the background
        mainHandler.post {
            (context as Activity).finish()
        }
    }

    override fun onActivityStopped(activity: Activity) {
        mainHandler.post {
            // debounce so this can only ever occur once
            if (!triggered.getAndSet(true)) {
                log("onActivityStopped is finished, calling Bugsnag.notify in the background")
                Bugsnag.notify(generateException())
            }
        }
    }
}
