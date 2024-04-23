package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration

private const val MAX_BREADCRUMB_COUNT = 500

class CXXMaxBreadcrumbCrashScenario(
    config: Configuration,
    context: Context,
    eventMetadata: String?
) : Scenario(config, context, eventMetadata) {
    init {
        config.maxBreadcrumbs = MAX_BREADCRUMB_COUNT
    }

    external fun activate()

    override fun startScenario() {
        super.startScenario()
        repeat(config.maxBreadcrumbs) { index ->
            Bugsnag.leaveBreadcrumb("this is breadcrumb $index")
        }

        activate()
    }
}
