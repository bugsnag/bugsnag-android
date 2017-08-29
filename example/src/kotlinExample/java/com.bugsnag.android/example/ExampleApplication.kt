package com.bugsnag.android.example

import android.app.Application

import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration

class ExampleApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize the Bugsnag client
        val configuration = Configuration("f35a2472bd230ac0ab0f52715bbdc65d")
        configuration.launchCrashThresholdMs = 20000
        Bugsnag.init(this, configuration)

    }

}
