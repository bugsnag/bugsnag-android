package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration
import com.bugsnag.android.MetaData

/**
 * Sets a new metadata on the object (regression testing for a previous NDK crash)
 */
internal class CustomMetaDataScenario(config: Configuration,
                                      context: Context) : Scenario(config, context) {

    override fun run() {
        super.run()
        Bugsnag.setMetaData(MetaData())
        Bugsnag.notify(generateException())
    }

}
