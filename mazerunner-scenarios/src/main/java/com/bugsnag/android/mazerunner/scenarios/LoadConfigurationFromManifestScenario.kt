package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import android.util.Log

import com.bugsnag.android.BreadcrumbType
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Client
import com.bugsnag.android.Configuration
import com.bugsnag.android.Event
import com.bugsnag.android.OnErrorCallback
import java.lang.RuntimeException


internal class LoadConfigurationFromManifestScenario(
    config: Configuration,
    context: Context
) : Scenario(config, context) {

    override fun run() {
        super.run()
        var testConfig = Configuration.load(this.context)
        testConfig.addOnError(OnErrorCallback { event ->
            event.addMetadata("test", "foo", "bar")
            event.addMetadata("test", "filter_me", "foobar")
            true
        })

        Bugsnag.start(this.context, testConfig)

        Bugsnag.leaveBreadcrumb("Test breadcrumb 1")
        Bugsnag.leaveBreadcrumb("Test breadcrumb 2")
        Bugsnag.leaveBreadcrumb("Test breadcrumb 3")
        Bugsnag.notify(RuntimeException("LoadConfigurationFromManifestScenario"))
    }
}
