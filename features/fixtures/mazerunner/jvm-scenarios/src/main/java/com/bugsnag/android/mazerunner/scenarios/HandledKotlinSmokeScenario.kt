package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration
import com.bugsnag.android.OnBreadcrumbCallback
import com.bugsnag.android.OnErrorCallback
import com.bugsnag.android.Severity
import java.util.regex.Pattern

/**
 * Sends a handled exception to Bugsnag, which does not include session data.
 */
internal class HandledKotlinSmokeScenario(
    config: Configuration,
    context: Context,
    eventMetadata: String?
) : Scenario(config, context, eventMetadata) {

    init {
        config.appType = "Overwritten"
        config.appVersion = "9.9.9"
        config.versionCode = 999
        config.releaseStage = "HandledKotlinSmokeScenario"
        config.enabledReleaseStages = setOf("HandledKotlinSmokeScenario")
        config.context = "HandledKotlinSmokeScenario"
        config.setUser("ABC", "ABC@CBA.CA", "HandledKotlinSmokeScenario")
        config.addMetadata("TestData", "Source", "HandledKotlinSmokeScenario")
        config.redactedKeys = setOf(Pattern.compile(".*redacted.*"))
        config.addOnBreadcrumb(
            OnBreadcrumbCallback {
                it.metadata?.put("Source", "HandledKotlinSmokeScenario")
                true
            }
        )
        config.addOnError(
            OnErrorCallback {
                it.addMetadata("TestData", "Callback", true)
                it.addMetadata("TestData", "redacted", false)
                it.severity = Severity.ERROR
                true
            }
        )
    }

    override fun startScenario() {
        super.startScenario()
        Bugsnag.leaveBreadcrumb("HandledKotlinSmokeScenario")
        Bugsnag.notify(generateException())
    }
}
