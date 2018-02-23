package com.bugsnag.android.mazerunner.scenarios

/**
 * Triggers a StackOverflow by recursing infinitely
 */
internal class StackOverflowScenario : Scenario() {

    override fun run() {
        calculateValue(0)
    }

    private fun calculateValue(count: Long): Long {
        return calculateValue(count + 1) + calculateValue(count - 1)
    }
}
