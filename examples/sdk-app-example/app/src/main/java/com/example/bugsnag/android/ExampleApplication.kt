package com.example.bugsnag.android

import android.app.Application
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration
import java.io.File

class ExampleApplication : Application() {

    companion object {
        init {
//            if you support API <= 17 you should uncomment this to load the bugsnag library
//            before any libraries that link to it
//            https://docs.bugsnag.com/platforms/android/#initialize-the-bugsnag-client
//
//            System.loadLibrary("bugsnag-ndk")
//            System.loadLibrary("bugsnag-plugin-android-anr")

            System.loadLibrary("entrypoint")
        }
    }

    private external fun performNativeBugsnagSetup()

    private val multiProcessApp = true

    override fun onCreate() {
        super.onCreate()


        val config = Configuration.load(this)
        config.setUser("123456", "joebloggs@example.com", "Joe Bloggs")
        config.addMetadata("user", "age", 31)

        if (multiProcessApp) {
            // alter the persistenceDirectory so that each process uses a unique directory
//            val processName = findCurrentProcessName()
//            config.persistenceDirectory = File(filesDir, processName)
        }

        Bugsnag.start(this, config)

        // Initialise native callbacks
        performNativeBugsnagSetup()
    }

}
