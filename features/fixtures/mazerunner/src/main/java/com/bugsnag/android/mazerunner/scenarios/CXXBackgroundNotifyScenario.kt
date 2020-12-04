package com.bugsnag.android.mazerunner.scenarios

import android.app.Activity
import android.content.Context

import com.bugsnag.android.Configuration

internal class CXXBackgroundNotifyScenario(
    config: Configuration,
    context: Context,
    eventMetadata: String
) : Scenario(config, context, eventMetadata) {

    init {
        System.loadLibrary("bugsnag-ndk")
        System.loadLibrary("entrypoint")
        config.autoTrackSessions = false
    }

    external fun activate()

    override fun startScenario() {
        super.startScenario()
        registerActivityLifecycleCallbacks()
    }

    override fun onActivityStopped(activity: Activity) = activate()
}
