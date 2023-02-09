package com.bugsnag.android.mazerunner

import android.app.Application
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration
import com.bugsnag.android.EndpointConfiguration
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

fun Application.triggerStartupAnrIfRequired() {
    val prefs = getSharedPreferences("AnrPreferences", Context.MODE_PRIVATE)
    val startupDelay = prefs.getLong("onCreateDelay", 0)

    if (startupDelay > 0L) {
        // Check if an endpoint configuration was added to the preferences
        val notifyEndpoint = prefs.getString("notify", null)
        val sessionsEndpoint = prefs.getString("sessions", null)

        // we remove the preferences so that we don't affect any future startup
        prefs.edit()
            .remove("onCreateDelay")
            .remove("notify")
            .remove("sessions")
            .commit()

        // we have to startup Bugsnag at this point
        val config = Configuration.load(this)
        if (!notifyEndpoint.isNullOrBlank() && !sessionsEndpoint.isNullOrBlank()) {
            config.endpoints = EndpointConfiguration(notifyEndpoint, sessionsEndpoint)
        }

        Bugsnag.start(this, config)

        // wait for Bugsnag's ANR handler to install first
        Handler(Looper.getMainLooper()).post {
            Log.i("StartupANR", "Going to sleep for $startupDelay seconds to trigger a startup ANR")
            thread {
                // This is a dirty hack to work around the limitations of our current testing
                // systems - where external key-events are pushed through our main thread (which we
                // are pausing to test for ANRs)

                // if there is a startup delay, we assume we are testing ANRs and send a "BACK"
                // key-press from the *system* in an attempt to cause an ANR
                try {
                    Thread.sleep(TimeUnit.SECONDS.toMillis(1L))
                    Runtime.getRuntime()
                        .exec("input keyevent 4")
                        .waitFor()
                } catch (ex: Exception) {
                    Log.w("StartupANR", "Couldn't send keyevent for BACK", ex)
                }
            }

            Thread.sleep(TimeUnit.SECONDS.toMillis(startupDelay))
        }
    }
}
