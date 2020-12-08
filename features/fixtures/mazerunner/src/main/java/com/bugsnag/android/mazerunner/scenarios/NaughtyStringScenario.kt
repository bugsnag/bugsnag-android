package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration
import com.bugsnag.android.mazerunner.addNaughtyStringMetadata

/**
 * Sends a handled exception to Bugsnag containing unusual string data
 * from https://github.com/minimaxir/big-list-of-naughty-strings
 */
internal class NaughtyStringScenario(
    config: Configuration,
    context: Context
) : Scenario(config, context) {
    init {
        config.autoTrackSessions = false
    }

    override fun run() {
        super.run()
        addNaughtyStringMetadata(javaClass)
        Bugsnag.notify(generateException())
    }
}
