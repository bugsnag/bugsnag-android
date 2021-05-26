package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration
import com.bugsnag.android.setAutoDetectAnrs

internal class AutoDetectAnrsTrueScenario(
    config: Configuration,
    context: Context,
    eventMetadata: String?
) : Scenario(config, context, eventMetadata) {

    init {
        config.enabledErrorTypes.anrs = true
    }

    override fun startScenario() {
        super.startScenario()
        setAutoDetectAnrs(Bugsnag.getClient(), false)
        setAutoDetectAnrs(Bugsnag.getClient(), true)

        val main = Handler(Looper.getMainLooper())
        main.postDelayed(
            Runnable {
                Thread.sleep(100000)
            },
            1
        ) // A moment of delay so there is something to 'tap' onscreen
    }
}
