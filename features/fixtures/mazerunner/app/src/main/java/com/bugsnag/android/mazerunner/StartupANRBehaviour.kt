package com.bugsnag.android.mazerunner

import android.app.Application
import android.content.Context
import android.util.Log
import com.bugsnag.android.Bugsnag
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

fun Application.triggerStartupAnrIfRequired() {
    val prefs = getSharedPreferences("AnrPreferences", Context.MODE_PRIVATE)
    val startupDelay = prefs.getLong("onCreateDelay", 0)

    if (startupDelay > 0L) {
        // we remove the preference so that we don't affect any future startup
        prefs.edit()
            .remove("onCreateDelay")
            .commit()

        // we have to startup Bugsnag at this point
        Bugsnag.start(this)

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
