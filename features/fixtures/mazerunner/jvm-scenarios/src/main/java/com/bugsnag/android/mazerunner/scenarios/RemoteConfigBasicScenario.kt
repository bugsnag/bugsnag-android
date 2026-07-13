package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.EndpointConfiguration
import com.bugsnag.android.Configuration
import java.io.IOException

class RemoteConfigBasicScenario(
    config: Configuration,
    context: Context,
    eventMetadata: String
) : Scenario(config, context, eventMetadata) {
    val handler = Handler(Looper.getMainLooper())

    init {
        if (eventMetadata == "disable-remote-config") {
            config.endpoints = EndpointConfiguration(
                config.endpoints.notify,
                config.endpoints.sessions
            )
        }
    }

    override fun startScenario() {
        super.startScenario()
        Bugsnag.notify(RuntimeException("Handled exception"))

        Thread {
            Thread.sleep(3000)
            throw IOException("Unhandled exception")
        }.start()
    }
}
