package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.Configuration
import com.bugsnag.android.mazerunner.addNaughtyStringMetadata

internal class CXXNaughtyStringsScenario(
    config: Configuration,
    context: Context
) : Scenario(config, context) {

    init {
        System.loadLibrary("entrypoint")
        config.autoTrackSessions = false
    }

    external fun crash()

    override fun run() {
        super.run()
        addNaughtyStringMetadata(javaClass)
        Thread.sleep(200) // allow metadata to sync across JNI layer

        if (eventMetaData != "non-crashy") {
            crash()
        }
    }
}
