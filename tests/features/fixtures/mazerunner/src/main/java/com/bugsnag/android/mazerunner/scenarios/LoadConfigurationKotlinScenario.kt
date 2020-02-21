package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import android.util.Log

import com.bugsnag.android.BreadcrumbType
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Client
import com.bugsnag.android.Configuration
import com.bugsnag.android.CustomPluginExample
import com.bugsnag.android.EndpointConfiguration
import com.bugsnag.android.Event
import com.bugsnag.android.OnErrorCallback
import java.lang.RuntimeException


internal class LoadConfigurationKotlinScenario(
    config: Configuration,
    context: Context
) : Scenario(config, context) {

    init {
    }

    override fun run() {
        super.run()
        var testConfig = Configuration("12312312312312312312312312312312")
        testConfig.appVersion  = "7.5.3"
        testConfig.appType = "test"
        testConfig.autoDetectErrors = true
        testConfig.autoTrackSessions = false
        testConfig.buildUuid = "test-7.5.3"
        testConfig.enabledReleaseStages = setOf("production", "development", "testing")
        testConfig.endpoints = EndpointConfiguration("http://bs-local.com:9339", "http://bs-local.com:9339")
        testConfig.projectPackages = setOf("com.company.package1", "com.company.package2")
        testConfig.discardClasses = setOf("java.net.UnknownHostException", "com.example.Custom")
        testConfig.launchCrashThresholdMs = 10000
        testConfig.maxBreadcrumbs = 1
        testConfig.persistUser = false
        testConfig.redactedKeys = setOf("filter_me")
        testConfig.releaseStage = "testing"
        testConfig.versionCode = 753
        testConfig.addOnError(OnErrorCallback { event ->
            event.addMetadata("test", "foo", "bar")
            event.addMetadata("test", "filter_me", "foobar")
            true
        })

        Bugsnag.start(this.context, testConfig)

        Bugsnag.leaveBreadcrumb("Test breadcrumb 1")
        Bugsnag.leaveBreadcrumb("Test breadcrumb 2")
        Bugsnag.leaveBreadcrumb("Test breadcrumb 3")
        Bugsnag.notify(RuntimeException("LoadConfigurationKotlinScenario"))
    }
}
