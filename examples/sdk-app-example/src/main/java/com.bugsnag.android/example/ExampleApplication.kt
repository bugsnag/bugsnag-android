package com.bugsnag.android.example

import android.app.Application
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration
import com.bugsnag.android.ErrorTypes

class ExampleApplication : Application() {

    companion object {
        init {
            System.loadLibrary("entrypoint")
        }
    }

    private external fun performNativeBugsnagSetup()

    override fun onCreate() {
        super.onCreate()

        val config = Configuration.load(this)
        config.enabledErrorTypes.ndkCrashes = true
        config.setUser("123456", "joebloggs@example.com", "Joe Bloggs")
        config.addMetadata("user", "age", 31)
        Bugsnag.start(this, config)

        // Initialise native callbacks
        performNativeBugsnagSetup()
    }

}
