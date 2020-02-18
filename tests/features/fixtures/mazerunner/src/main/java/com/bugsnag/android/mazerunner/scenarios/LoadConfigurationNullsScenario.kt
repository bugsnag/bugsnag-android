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


internal class LoadConfigurationNullsScenario(
    config: Configuration,
    context: Context
) : Scenario(config, context) {

    init {
    }

    override fun run() {
        super.run()
        var testConfig = Configuration("12312312312312312312312312312312")
        // Setup
        testConfig.autoDetectErrors = true
        testConfig.autoTrackSessions = false
        testConfig.endpoints = EndpointConfiguration("http://bs-local.com:9339", "http://bs-local.com:9339")

        // Nullable options
        testConfig.appVersion  = null
        testConfig.buildUuid = null
        testConfig.enabledReleaseStages = null
        testConfig.codeBundleId = null
        testConfig.delivery = null
        testConfig.context = null
        testConfig.enabledBreadcrumbTypes = null
        testConfig.releaseStage = null
        testConfig.versionCode = null

        testConfig.addOnError(OnErrorCallback { event ->
            event.addMetadata("test", "foo", "bar")
            event.addMetadata("test", "filter_me", "foobar")
            true
        })

        Bugsnag.start(this.context, testConfig)

        Bugsnag.notify(RuntimeException("LoadConfigurationNullsScenario"))
    }
}
