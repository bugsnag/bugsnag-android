package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration

/**
 * Sends a handled exception to Bugsnag, which overrides the app version
 */
internal class AppVersionScenario(config: Configuration,
                                  context: Context) : Scenario(config, context) {

    override fun run() {
        super.run()
        Bugsnag.setAppVersion("1.2.3.abc")
        Bugsnag.notify(generateException())
    }

}
