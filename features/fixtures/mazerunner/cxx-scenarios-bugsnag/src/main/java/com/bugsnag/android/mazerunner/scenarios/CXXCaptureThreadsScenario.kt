package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.Configuration
import com.bugsnag.android.ThreadSendPolicy

class CXXCaptureThreadsScenario(
    config: Configuration,
    context: Context,
    eventMetadata: String?
) : Scenario(config, context, eventMetadata) {
    companion object {
        init {
            System.loadLibrary("bugsnag-ndk")
            System.loadLibrary("cxx-scenarios-bugsnag")
        }
    }

    init {
        if (!eventMetadata.isNullOrEmpty()) {
            config.sendThreads = ThreadSendPolicy.valueOf(eventMetadata)
        }
    }

    external fun crash()

    override fun startScenario() {
        super.startScenario()
        crash()
    }
}
