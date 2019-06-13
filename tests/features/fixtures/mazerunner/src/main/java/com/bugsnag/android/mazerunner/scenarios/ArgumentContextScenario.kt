package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration
import com.bugsnag.android.MetaData
import com.bugsnag.android.Severity
import com.bugsnag.android.mazerunner.scenarios.Scenario

/**
 * Sends a handled exception to Bugsnag, which includes manual context.
 */
internal class ArgumentContextScenario(config: Configuration,
                                       context: Context) : Scenario(config, context) {
    override fun run() {
        super.run()
        Bugsnag.getClient().notify("RuntimeException", "deprecated", "NewContext", emptyArray<StackTraceElement>(), Severity.ERROR, MetaData())
    }

}
