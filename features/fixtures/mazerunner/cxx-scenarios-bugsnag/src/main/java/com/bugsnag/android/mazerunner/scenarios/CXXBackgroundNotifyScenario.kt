package com.bugsnag.android.mazerunner.scenarios

import android.app.Activity
import android.content.Context

import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration

internal class CXXBackgroundNotifyScenario(
    config: Configuration,
    context: Context
) : Scenario(config, context) {

    init {
        System.loadLibrary("bugsnag-ndk")
        System.loadLibrary("cxx-scenarios-bugsnag")
        config.autoTrackSessions = false
    }

    external fun activate()

    override fun run() {
        super.run()
        registerActivityLifecycleCallbacks()
    }

    override fun onActivityStopped(activity: Activity) = activate()
}
