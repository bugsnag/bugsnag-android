package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.BreadcrumbType
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration

/**
 * Sends a handled exception to Bugsnag, which includes manual breadcrumbs.
 */
internal class BreadcrumbScenario(
    config: Configuration,
    context: Context,
    eventMetadata: String
) : Scenario(config, context, eventMetadata) {

    init {
        config.enabledBreadcrumbTypes = setOf(BreadcrumbType.MANUAL, BreadcrumbType.USER)
    }

    override fun startScenario() {
        super.startScenario()
        Bugsnag.leaveBreadcrumb("Hello Breadcrumb!")
        val data = mapOf<String, Any?>(
            "Foo" to "Bar",
            "password" to "my password"
        )
        Bugsnag.leaveBreadcrumb("Another Breadcrumb", data, BreadcrumbType.USER)
        Bugsnag.notify(generateException())
    }
}
