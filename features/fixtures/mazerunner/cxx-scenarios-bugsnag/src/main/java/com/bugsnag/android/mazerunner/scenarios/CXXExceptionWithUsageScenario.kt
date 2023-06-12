package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration
import com.bugsnag.android.OnBreadcrumbCallback
import com.bugsnag.android.OnErrorCallback
import com.bugsnag.android.OnSessionCallback
import com.bugsnag.android.Telemetry
import java.util.regex.Pattern

class CXXExceptionWithUsageScenario(
    config: Configuration,
    context: Context,
    eventMetadata: String?
) : Scenario(config, context, eventMetadata) {

    companion object {
        init {
            System.loadLibrary("cxx-scenarios-bugsnag")
        }
    }

    init {
        if (eventMetadata != "USAGE") {
            config.setTelemetry(config.getTelemetry().filter { it != Telemetry.USAGE }.toSet())
        } else {
            config.setTelemetry(config.getTelemetry() + Telemetry.USAGE)
        }
        config.maxBreadcrumbs = 10
        config.autoTrackSessions = false
        config.discardClasses =
            setOf<Pattern>(
                Pattern.compile("one"),
                Pattern.compile("two"),
                Pattern.compile("three")
            )

        config.maxPersistedSessions = 1000
        val breadcrumbCb = OnBreadcrumbCallback { true }
        config.addOnBreadcrumb(breadcrumbCb)
        config.removeOnBreadcrumb(breadcrumbCb)
        config.removeOnBreadcrumb(breadcrumbCb)
        config.addOnBreadcrumb(OnBreadcrumbCallback { true })
        config.addOnError(OnErrorCallback { true })
        config.addOnError(OnErrorCallback { true })
        config.addOnSession(OnSessionCallback { true })
        config.addOnSession(OnSessionCallback { true })
        config.addOnSession(OnSessionCallback { true })
    }

    external fun crash()

    override fun startScenario() {
        super.startScenario()
        Bugsnag.addOnSession { true }
        crash()
    }
}
