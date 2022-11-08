package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration
import com.bugsnag.android.FeatureFlag

class FeatureFlagScenario(
    config: Configuration,
    context: Context,
    eventMetadata: String?
) : Scenario(config, context, eventMetadata) {

    init {
        if (eventMetadata?.contains("onsend") == true) {
            config.addOnSend { event ->
                event.addFeatureFlag("on_send_callback")
                return@addOnSend true
            }
        }

        config.addFeatureFlag("demo_mode")
    }

    override fun startScenario() {
        super.startScenario()

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
