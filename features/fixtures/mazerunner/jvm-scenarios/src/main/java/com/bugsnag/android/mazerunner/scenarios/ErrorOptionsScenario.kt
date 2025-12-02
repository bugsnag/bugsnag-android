package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration
import com.bugsnag.android.ErrorCaptureOptions
import com.bugsnag.android.ErrorOptions

class ErrorOptionsScenario(
    config: Configuration,
    context: Context,
    eventMetadata: String?
) : Scenario(config, context, eventMetadata) {
    private val errorOptions = ErrorOptions(ErrorCaptureOptions.captureNothing())

    init {
        val enabledCapture = eventMetadata?.splitToSequence(' ')
            ?.map { it.trim() }
            ?.toMutableSet()
            ?: mutableSetOf()

        // remove each of the options - if they were present set the associated capture option
        errorOptions.capture.stacktrace = enabledCapture.remove("stacktrace")
        errorOptions.capture.breadcrumbs = enabledCapture.remove("breadcrumbs")
        errorOptions.capture.featureFlags = enabledCapture.remove("featureFlags")
        errorOptions.capture.threads = enabledCapture.remove("threads")
        errorOptions.capture.user = enabledCapture.remove("user")

        // any remaining options are used for metadata tabs
        errorOptions.capture.metadata = enabledCapture
    }

    override fun startScenario() {
        super.startScenario()
        Bugsnag.addMetadata("custom", "key1", "value")
        Bugsnag.addMetadata("custom2", "testKey2", "value")
        Bugsnag.leaveBreadcrumb("Test breadcrumb")
        Bugsnag.addFeatureFlag("testFeatureFlag", "variantA")
        Bugsnag.addFeatureFlag("featureFlag2")
        Bugsnag.setUser("123", "jane@doe.com", "Jane Doe")
        Bugsnag.notify(generateException(), errorOptions, null)
    }
}
