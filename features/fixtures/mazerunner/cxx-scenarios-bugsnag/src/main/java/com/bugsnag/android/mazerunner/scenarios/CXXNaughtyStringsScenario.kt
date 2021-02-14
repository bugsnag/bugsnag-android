package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.Configuration
import com.bugsnag.android.mazerunner.addNaughtyStringMetadata

internal class CXXNaughtyStringsScenario(
    config: Configuration,
    context: Context,
    eventMetadata: String?
) : Scenario(config, context, eventMetadata) {

    init {
        System.loadLibrary("cxx-scenarios")
        config.autoTrackSessions = false
    }

    external fun crash()

    override fun startScenario() {
        super.startScenario()
        addNaughtyStringMetadata(javaClass)
        Thread.sleep(200) // allow metadata to sync across JNI layer

        crash()
    }
}
