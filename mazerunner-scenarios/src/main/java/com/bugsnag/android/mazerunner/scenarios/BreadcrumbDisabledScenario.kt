package com.bugsnag.android.mazerunner.scenarios

import android.content.Context

import com.bugsnag.android.BreadcrumbType
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration

import java.util.*

/**
 * Sends a handled exception to Bugsnag, which includes manual breadcrumbs.
 */
internal class BreadcrumbDisabledScenario(config: Configuration,
                                  context: Context) : Scenario(config, context) {
    init {
        config.autoTrackSessions = false
        config.enabledBreadcrumbTypes = emptySet()
    }

    override fun run() {
        super.run()
        Bugsnag.leaveBreadcrumb("Hello Breadcrumb!")
        Bugsnag.notify(generateException())
    }

}
