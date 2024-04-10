package com.bugsnag.android.mazerunner

import android.app.Application
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration
import com.bugsnag.android.EndpointConfiguration

fun Application.triggerManualSessionIfRequired() {
    val prefs = getSharedPreferences("SessionPreferences", android.content.Context.MODE_PRIVATE)
    val manualSession = prefs.getBoolean("manualSession", false)

    if (manualSession) {
        val notifyEndpoint = prefs.getString("notify", null)
        val sessionsEndpoint = prefs.getString("sessions", null)

        // we remove the preferences so that we don't affect any future startup
        prefs.edit()
            .remove("notify")
            .remove("sessions")
            .remove("manualSession")
            .commit()

        // we have to startup Bugsnag at this point
        val config = Configuration.load(this)
        if (!notifyEndpoint.isNullOrBlank() && !sessionsEndpoint.isNullOrBlank()) {
            config.endpoints = EndpointConfiguration(notifyEndpoint, sessionsEndpoint)
        }

        Bugsnag.start(this, config)
        Bugsnag.startSession()
    }
}
