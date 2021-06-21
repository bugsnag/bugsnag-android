package com.bugsnag.android.anrapp

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration
import java.lang.Exception
import java.lang.Thread.sleep
import kotlin.concurrent.thread
import kotlin.math.min

class AnrApplication : Application() {
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)

        if (preferences.getBoolean("startOnBackground", false)) {
            thread(isDaemon = true) {
                startBugsnag()
            }
        } else {
            startBugsnag()
        }

        preferences.edit()
            .remove("startOnBackground")
            .apply()
    }

    private fun startBugsnag() = withLogMessage("Calling Bugsnag.start") {
        Bugsnag.start(this, Configuration.load(this))
    }

    override fun onCreate() {
        super.onCreate()
        val startupDelay = preferences.getLong("onCreateDelay", 0)
        preferences.edit()
            .remove("onCreateDelay")
            .apply()

        if (startupDelay > 0L) {
            thread {
                // This is a dirty hack to work around the limitations of our current testing
                // systems - where external key-events are pushed through our main thread (which we
                // are pausing to test for ANRs)

                // if there is a startup delay, we assume we are testing ANRs and send a "BACK"
                // key-press from the *system* in an attempt to cause an ANR
                try {
                    sleep(min(startupDelay / 10L, 1_000L))
                    withLogMessage("Sending BACK key event") {
                        Runtime.getRuntime()
                            .exec("input keyevent 4")
                            .waitFor()
                    }
                } catch (ex: Exception) {
                    Log.w(LOG_TAG, "Couldn't send keyevent for BACK", ex)
                }
            }

            withLogMessage("onCreate delay for $startupDelay seconds") {
                sleep(startupDelay * 1000L)
            }
        }
    }
}

val Application.preferences: SharedPreferences
    get() = getSharedPreferences("Preferences", Context.MODE_PRIVATE)
