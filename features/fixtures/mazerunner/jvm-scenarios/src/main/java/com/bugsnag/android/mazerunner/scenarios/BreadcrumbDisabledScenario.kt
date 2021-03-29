package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration

/**
 * Sends a handled exception to Bugsnag, which includes manual breadcrumbs.
 */
internal class BreadcrumbDisabledScenario(
    config: Configuration,
    context: Context,
    eventMetadata: String
) : Scenario(config, context, eventMetadata) {

    init {
        config.enabledBreadcrumbTypes = emptySet()
    }

    override fun startScenario() {
        super.startScenario()
        Bugsnag.leaveBreadcrumb("Hello Breadcrumb!")
        Bugsnag.notify(generateException())
    }
}
