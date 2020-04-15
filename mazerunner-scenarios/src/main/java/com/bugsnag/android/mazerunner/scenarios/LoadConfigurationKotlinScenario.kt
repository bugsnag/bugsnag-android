package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import android.util.Log

import com.bugsnag.android.BreadcrumbType
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Client
import com.bugsnag.android.Configuration
import com.bugsnag.android.EndpointConfiguration
import com.bugsnag.android.Event
import com.bugsnag.android.OnErrorCallback
import com.bugsnag.android.ThreadSendPolicy
import java.lang.RuntimeException


internal class LoadConfigurationKotlinScenario(
    config: Configuration,
    context: Context
) : Scenario(config, context) {

    override fun run() {
        super.run()
        var testConfig = Configuration("78978978978978978978978978978978")
        testConfig.apiKey = "45645645645645645645645645645645"
        testConfig.appVersion  = "0.9.8"
        testConfig.appType = "kotlin"
        testConfig.autoDetectErrors = true
        testConfig.autoTrackSessions = false
        testConfig.enabledReleaseStages = setOf("production", "development", "kotlin")
        testConfig.endpoints = EndpointConfiguration("http://bs-local.com:9339", "http://bs-local.com:9339")
        testConfig.projectPackages = setOf("com.company.package1", "com.company.package2")
        testConfig.discardClasses = setOf("java.net.UnknownHostException", "com.example.Custom")
        testConfig.launchCrashThresholdMs = 10000
        testConfig.maxBreadcrumbs = 1
        testConfig.persistUser = false
        testConfig.redactedKeys = setOf("filter_me_two")
        testConfig.releaseStage = "kotlin"
        testConfig.sendThreads = ThreadSendPolicy.NEVER
        testConfig.versionCode = 98
        testConfig.addOnError(OnErrorCallback { event ->
            event.addMetadata("test", "filter_me", "bar")
            event.addMetadata("test", "filter_me_two", "foobar")
            true
        })

        Bugsnag.start(this.context, testConfig)

        Bugsnag.leaveBreadcrumb("Test breadcrumb 1")
        Bugsnag.leaveBreadcrumb("Test breadcrumb 2")
        Bugsnag.leaveBreadcrumb("Test breadcrumb 3")
        Bugsnag.notify(RuntimeException("LoadConfigurationKotlinScenario"))
    }
}
