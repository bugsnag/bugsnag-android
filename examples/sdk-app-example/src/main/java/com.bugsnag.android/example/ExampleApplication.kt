package com.bugsnag.android.example

import android.app.Application
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration
import com.bugsnag.android.ErrorTypes

class ExampleApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        val config = Configuration.load(this)
        config.enabledErrorTypes.ndkCrashes = true
        Bugsnag.start(this, config)
    }

}
