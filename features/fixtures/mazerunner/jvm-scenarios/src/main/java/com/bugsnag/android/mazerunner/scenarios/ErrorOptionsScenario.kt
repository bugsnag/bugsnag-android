package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.CaptureOptions
import com.bugsnag.android.Configuration
import com.bugsnag.android.ErrorOptions

class ErrorOptionsScenario(
    config: Configuration,
    context: Context,
    eventMetadata: String?
) : Scenario(config, context, eventMetadata) {
    private val errorOptions = ErrorOptions(CaptureOptions.captureNothing())

    override fun startScenario() {
        super.startScenario()
        Bugsnag.addMetadata("custom", "key", "value")
        Bugsnag.leaveBreadcrumb("Test breadcrumb")
        Bugsnag.addFeatureFlag("testFeatureFlag", "variantA")
        Bugsnag.setUser("123", "jane@doe.com", "Jane Doe")
        Bugsnag.notify(generateException(), errorOptions, null)
    }
}
