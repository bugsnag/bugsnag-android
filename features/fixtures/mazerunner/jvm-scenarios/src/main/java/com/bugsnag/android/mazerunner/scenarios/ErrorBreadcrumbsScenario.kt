package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.BreadcrumbType
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration

class ErrorBreadcrumbsScenario(
    config: Configuration,
    context: Context,
    eventMetadata: String?
) : Scenario(
    config.apply {
        enabledBreadcrumbTypes = setOf(BreadcrumbType.ERROR)
    },
    context,
    eventMetadata
) {
    override fun startScenario() {
        Bugsnag.notify(RuntimeException("first error"))
        throw NullPointerException("something broke")
    }
}
