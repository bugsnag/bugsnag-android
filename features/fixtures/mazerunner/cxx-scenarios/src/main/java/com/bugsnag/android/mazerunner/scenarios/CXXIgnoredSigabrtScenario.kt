package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.Configuration
import java.util.regex.Pattern

internal class CXXIgnoredSigabrtScenario(
    config: Configuration,
    context: Context,
    eventMetadata: String
) : Scenario(config, context, eventMetadata) {

    init {
        config.discardClasses = setOf(Pattern.compile(eventMetadata))
        System.loadLibrary("cxx-scenarios")
    }

    external fun crash(value: Int): Int

    override fun startScenario() {
        super.startScenario()
        crash(2726)
    }
}
