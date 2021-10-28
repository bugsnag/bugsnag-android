package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.BreadcrumbType
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration
import java.util.Collections

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
        val data = Collections.singletonMap("Foo", "Bar" as Any)
        Bugsnag.leaveBreadcrumb("Another Breadcrumb", data, BreadcrumbType.USER)
        Bugsnag.notify(generateException())
    }
}
