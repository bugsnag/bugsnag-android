package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration
import com.bugsnag.android.OnBreadcrumbCallback
import com.bugsnag.android.OnErrorCallback
import com.bugsnag.android.OnSessionCallback
import com.bugsnag.android.Telemetry

class UnhandledExceptionWithUsageScenario(
    config: Configuration,
    context: Context,
    eventMetadata: String
) : Scenario(config, context, eventMetadata) {

    companion object {
        init {
            System.loadLibrary("cxx-scenarios-bugsnag")
        }
    }

    val onSessionCallback = OnSessionCallback { true }

    init {
        if (eventMetadata != "USAGE") {
            config.setTelemetry(config.getTelemetry().filter { it != Telemetry.USAGE }.toSet())
        } else {
            config.setTelemetry(config.getTelemetry() + Telemetry.USAGE)
        }
        config.maxBreadcrumbs = 10
        config.autoTrackSessions = false
        val breadcrumbCb = OnBreadcrumbCallback { true }
        config.addOnBreadcrumb(breadcrumbCb)
        config.removeOnBreadcrumb(breadcrumbCb)
        config.removeOnBreadcrumb(breadcrumbCb)
        config.addOnBreadcrumb(OnBreadcrumbCallback { true })
        config.addOnError(OnErrorCallback { true })
        config.addOnError(OnErrorCallback { true })
        config.addOnSession(OnSessionCallback { true })
        config.addOnSession(OnSessionCallback { true })
        config.addOnSession(onSessionCallback)
    }

    external fun cxxsetup()

    override fun startScenario() {
        super.startScenario()
        cxxsetup()
        Bugsnag.addOnBreadcrumb { true }
        Bugsnag.addOnBreadcrumb { true }
        Bugsnag.removeOnSession(onSessionCallback)
        throw generateException()
    }
}
