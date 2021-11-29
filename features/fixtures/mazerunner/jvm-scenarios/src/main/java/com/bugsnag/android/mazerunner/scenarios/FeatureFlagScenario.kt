package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration
import com.bugsnag.android.FeatureFlag

class FeatureFlagScenario(
    config: Configuration,
    context: Context,
    eventMetadata: String
) : Scenario(config, context, eventMetadata) {
    override fun startScenario() {
        super.startScenario()

        Bugsnag.addFeatureFlag("demo_mode")

        Bugsnag.addFeatureFlags(
            listOf(
                FeatureFlag("should_not_be_reported_1"),
                FeatureFlag("should_not_be_reported_2"),
                FeatureFlag("should_not_be_reported_3")
            )
        )

        Bugsnag.clearFeatureFlag("should_not_be_reported_3")
        Bugsnag.clearFeatureFlag("should_not_be_reported_2")
        Bugsnag.clearFeatureFlag("should_not_be_reported_1")

        if (eventMetadata?.contains("callback") == true) {
            Bugsnag.addOnError { error ->
                error.addFeatureFlag("sample_group", "a")
                return@addOnError true
            }
        }

        if (eventMetadata?.contains("cleared") == true) {
            Bugsnag.clearFeatureFlags()
        }

        if (eventMetadata?.contains("unhandled") == true) {
            throw RuntimeException("FeatureFlagScenario unhandled")
        } else {
            Bugsnag.notify(RuntimeException("FeatureFlagScenario handled"))
        }
    }
}
