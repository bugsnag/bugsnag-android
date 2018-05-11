package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration
import com.bugsnag.android.disableSessionDelivery

/**
 * Sends a handled exception to Bugsnag, which includes session data.
 */
internal class HandledExceptionSessionScenario(config: Configuration,
                                               context: Context) : Scenario(config, context) {

    override fun run() {
        super.run()
        disableSessionDelivery(Bugsnag.getClient())
        Bugsnag.startSession()
        Bugsnag.notify(generateException())
    }

}
