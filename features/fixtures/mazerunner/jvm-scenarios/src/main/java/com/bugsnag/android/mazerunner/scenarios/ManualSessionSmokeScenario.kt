package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration

/**
 * Sends an exception after pausing the session
 */
internal class ManualSessionSmokeScenario(
    config: Configuration,
    context: Context,
    eventMetadata: String
) : Scenario(config, context, eventMetadata) {

    override fun startBugsnag(startBugsnagOnly: Boolean) {
        super.startBugsnag(startBugsnagOnly)
    }

    override fun startScenario() {
        super.startScenario()
        Bugsnag.setUser("123", "ABC.CBA.CA", "ManualSessionSmokeScenario")

        // send session
        Bugsnag.startSession()

        // send exception with session
        Bugsnag.notify(generateException())

        // send exception without session
        Bugsnag.pauseSession()
        Bugsnag.notify(generateException())

        // throw exception with session
        Bugsnag.resumeSession()
        throw generateException()
    }
}
