package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.BreadcrumbType
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration
import java.util.*

/**
 * Sends a handled exception to Bugsnag, which includes manual breadcrumbs.
 */
internal class BreadcrumbScenario(config: Configuration,
                                  context: Context) : Scenario(config, context) {

    override fun run() {
        super.run()
        Bugsnag.leaveBreadcrumb("Hello Breadcrumb!")
        Bugsnag.leaveBreadcrumb("Another Breadcrumb", BreadcrumbType.USER, Collections.singletonMap("Foo", "Bar"))
        Bugsnag.notify(generateException())
    }

}
