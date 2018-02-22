package com.bugsnag.android.mazerunner.scenarios

/**
 * Triggers a StackOverflow by recursing infinitely
 */
internal class StackOverflowScenario : Scenario() {

    override fun run() {
        run()
    }

}
