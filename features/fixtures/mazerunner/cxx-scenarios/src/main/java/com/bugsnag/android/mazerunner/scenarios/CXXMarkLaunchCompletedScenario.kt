package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration
import com.bugsnag.android.createDefaultDelivery
import com.bugsnag.android.mazerunner.InterceptingDelivery

/**
 * Sends an NDK error to Bugsnag after markLaunchCompleted() is invoked.
 */
internal class CXXMarkLaunchCompletedScenario(
    config: Configuration,
    context: Context,
    eventMetadata: String?
) : Scenario(config, context, eventMetadata) {

    external fun crash()

    init {
        System.loadLibrary("cxx-scenarios")

        config.launchDurationMillis = 0

        config.delivery = InterceptingDelivery(createDefaultDelivery()) { result ->
            Handler(Looper.getMainLooper()).post {
                crash()
            }
        }
    }

    override fun startScenario() {
        super.startScenario()
        Bugsnag.notify(RuntimeException("first error"))
        Bugsnag.markLaunchCompleted()
    }
}
