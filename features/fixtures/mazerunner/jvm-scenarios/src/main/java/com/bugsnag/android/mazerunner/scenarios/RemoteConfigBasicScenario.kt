package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration

class RemoteConfigBasicScenario(
    config: Configuration,
    context: Context,
    eventMetadata: String
) : Scenario(config, context, eventMetadata) {
    val handler = Handler(Looper.getMainLooper())

    override fun startScenario() {
        super.startScenario()
        Bugsnag.notify(RuntimeException("Handled exception"))
        handler.postDelayed({
            throw RuntimeException("Unhandled exception")
        }, 3000)
    }
}
