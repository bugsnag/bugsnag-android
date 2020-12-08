package com.bugsnag.android.mazerunner.scenarios

import com.bugsnag.android.Configuration

import android.content.Context
import android.content.Intent

/**
 * Sets automatic capture of sessions in Bugsnag and flushes 1 session
 */
internal class SessionSetAutoCaptureScenario(
    config: Configuration,
    context: Context,
    eventMetadata: String
) : Scenario(config, context, eventMetadata) {

    init {
        config.autoTrackSessions = true
    }

    override fun startScenario() {
        super.startScenario()
        context.startActivity(Intent("com.bugsnag.android.mazerunner.UPDATE_CONTEXT"))
    }

}
