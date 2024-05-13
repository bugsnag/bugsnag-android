package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration
import com.bugsnag.android.OnErrorCallback
import java.lang.RuntimeException

internal class LoadConfigurationFromManifestScenario(
    config: Configuration,
    context: Context,
    eventMetadata: String
) : Scenario(config, context, eventMetadata) {

    override fun startBugsnag(startBugsnagOnly: Boolean) {
        this.startBugsnagOnly = startBugsnagOnly
        val testConfig = Configuration.load(this.context)

        // Record the endpoints loaded from the manifest as metadata
        // and use the endpoints configured for the scenario
        val manifestEndpoints = testConfig.endpoints
        testConfig.endpoints = this.config.endpoints

        testConfig.addOnError(
            OnErrorCallback { event ->
                event.addMetadata("endpoints", "notify", manifestEndpoints.notify)
                event.addMetadata("endpoints", "sessions", manifestEndpoints.sessions)
                event.addMetadata("test", "foo", "bar")
                event.addMetadata("test", "filter_me", "foobar")
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
        Bugsnag.notify(RuntimeException("LoadConfigurationFromManifestScenario"))
    }
}
