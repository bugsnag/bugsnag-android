package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration
import com.bugsnag.android.EndpointConfiguration
import com.bugsnag.android.OnErrorCallback
import com.bugsnag.android.ThreadSendPolicy
import java.lang.RuntimeException
import java.util.regex.Pattern

internal class LoadConfigurationKotlinScenario(
    config: Configuration,
    context: Context,
    eventMetadata: String
) : Scenario(config, context, eventMetadata) {

    override fun startBugsnag(startBugsnagOnly: Boolean) {
        this.startBugsnagOnly = startBugsnagOnly
        val testConfig = Configuration("78978978978978978978978978978978")
        testConfig.apiKey = "45645645645645645645645645645645"
        testConfig.appVersion = "0.9.8"
        testConfig.appType = "kotlin"
        testConfig.autoDetectErrors = true
        testConfig.autoTrackSessions = false
        testConfig.enabledReleaseStages = setOf("production", "development", "kotlin")
        testConfig.endpoints =
            EndpointConfiguration(this.config.endpoints.notify, this.config.endpoints.sessions)
        testConfig.projectPackages = setOf("com.company.package1", "com.company.package2")
        testConfig.discardClasses = setOf(
            Pattern.compile(".*java.net.UnknownHostException.*"),
            Pattern.compile(".*com.example.Custom.*")
        )
        testConfig.launchDurationMillis = 10000
        testConfig.maxBreadcrumbs = 1
        testConfig.persistUser = false
        testConfig.redactedKeys = setOf(Pattern.compile(".*filter_me_two.*"))
        testConfig.releaseStage = "kotlin"
        testConfig.sendThreads = ThreadSendPolicy.NEVER
        testConfig.versionCode = 98
        testConfig.addOnError(
            OnErrorCallback { event ->
                event.addMetadata("test", "filter_me", "bar")
                event.addMetadata("test", "filter_me_two", "foobar")
                true
            }
        )

        measureBugsnagStartupDuration(this.context, testConfig)
    }

    override fun startScenario() {
        super.startScenario()
        Bugsnag.leaveBreadcrumb("Test breadcrumb 1")
        Bugsnag.leaveBreadcrumb("Test breadcrumb 2")
        Bugsnag.leaveBreadcrumb("Test breadcrumb 3")
        Bugsnag.notify(RuntimeException("LoadConfigurationKotlinScenario"))
    }
}
