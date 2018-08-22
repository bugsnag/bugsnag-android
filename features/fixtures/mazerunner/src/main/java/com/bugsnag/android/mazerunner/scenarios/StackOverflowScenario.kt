package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.Configuration

/**
 * Triggers a StackOverflow by recursing infinitely
 */
internal class StackOverflowScenario(config: Configuration,
                                     context: Context) : Scenario(config, context) {

    override fun run() {
        super.run()
        calculateValue(0)
    }

    private fun calculateValue(count: Long): Long {
        return calculateValue(count + 1) + calculateValue(count - 1)
    }
}
