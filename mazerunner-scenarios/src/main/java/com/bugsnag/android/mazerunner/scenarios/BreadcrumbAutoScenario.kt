package com.bugsnag.android.mazerunner.scenarios

import android.content.Context

import com.bugsnag.android.BreadcrumbType
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration

import java.util.*

/**
 * Sends a handled exception to Bugsnag, which includes manual breadcrumbs.
 */
internal class BreadcrumbAutoScenario(config: Configuration,
                                      context: Context) : Scenario(config, context) {
    init {
        config.autoTrackSessions = false
        config.enabledBreadcrumbTypes = setOf(BreadcrumbType.STATE)
    }

    override fun run() {
        super.run()
        Bugsnag.notify(generateException())
    }

}
