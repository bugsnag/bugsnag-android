package com.bugsnag.android.example

import android.app.Application

import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration

class ExampleApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize the Bugsnag client
        val configuration = Configuration("9f2996871fb381de73bfb0bea455c28b")
        configuration.setAutoCaptureSessions(true)
        configuration.sessionEndpoint = "http://10.0.2.2:1234"
        configuration.endpoint = "http://10.0.2.2:8000"
        Bugsnag.init(this, configuration)
    }

}
