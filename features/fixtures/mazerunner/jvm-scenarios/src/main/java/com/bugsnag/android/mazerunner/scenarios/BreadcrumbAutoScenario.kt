package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.BreadcrumbType
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration

/**
 * Sends a handled exception to Bugsnag, which includes manual breadcrumbs.
 */
internal class BreadcrumbAutoScenario(
    config: Configuration,
    context: Context,
    eventMetadata: String
) : Scenario(config, context, eventMetadata) {

    init {
        config.enabledBreadcrumbTypes = setOf(BreadcrumbType.STATE)
    }

    override fun startScenario() {
        super.startScenario()
        Bugsnag.notify(generateException())
    }
}
