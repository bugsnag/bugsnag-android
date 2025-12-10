package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration
import com.bugsnag.android.ErrorOptions

class NotifyFatalScenario(
    config: Configuration,
    context: Context,
    eventMetadata: String?
) : Scenario(config, context, eventMetadata) {
    private val errorOptions = ErrorOptions(isFatal = true)
    override fun startScenario() {
        super.startScenario()
        Bugsnag.addMetadata("custom", "key1", "value")
        Bugsnag.addMetadata("custom2", "testKey2", "value")
        Bugsnag.leaveBreadcrumb("Test breadcrumb")
        Bugsnag.addFeatureFlag("testFeatureFlag", "variantA")
        Bugsnag.addFeatureFlag("featureFlag2")
        Bugsnag.setUser("123", "jane@doe.com", "Jane Doe")
        Bugsnag.notify(generateException(), errorOptions, null)
        throw generateException()
    }
}
