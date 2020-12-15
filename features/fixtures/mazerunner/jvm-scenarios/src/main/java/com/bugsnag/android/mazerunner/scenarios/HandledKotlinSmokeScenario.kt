package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.*
import java.util.*

/**
 * Sends a handled exception to Bugsnag, which does not include session data.
 */
internal class HandledKotlinSmokeScenario(config: Configuration,
                                        context: Context) : Scenario(config, context) {
    init {
        config.autoTrackSessions = false
        config.appType = "Overwritten"
        config.appVersion = "9.9.9"
        config.versionCode = 999
        config.releaseStage = "HandledKotlinSmokeScenario"
        config.enabledReleaseStages = setOf("HandledKotlinSmokeScenario")
        config.context = "HandledKotlinSmokeScenario"
        config.setUser("ABC", "ABC@CBA.CA", "HandledKotlinSmokeScenario")
        config.addMetadata("TestData", "Source", "HandledKotlinSmokeScenario")
        config.redactedKeys = setOf("redacted")
        config.addOnBreadcrumb(OnBreadcrumbCallback {
            it.metadata?.put("Source", "HandledKotlinSmokeScenario")
            true
        })
        config.addOnError(OnErrorCallback {
            it.addMetadata("TestData", "Callback", true)
            it.addMetadata("TestData", "redacted", false)
            it.severity = Severity.ERROR
            true
        })
    }

    override fun run() {
        super.run()
        Bugsnag.leaveBreadcrumb("HandledKotlinSmokeScenario")
        Bugsnag.notify(generateException())
    }

}
