package com.bugsnag.android.mazerunner.scenarios

import android.app.Activity
import android.content.Context
import android.os.Handler
import com.bugsnag.android.*

/**
 * Generates a crash on startup, then relaunches the scenario with another crash, to verify that
 * the first crash is delivered synchronously regardless.
 */
internal class StartupCrashFlushScenario(config: Configuration,
                                         context: Context) : Scenario(config, context) {
    init {
        config.setAutoCaptureSessions(false)

        if (context is Activity) {
            eventMetaData = context.intent.getStringExtra("EVENT_METADATA")
            if ("CrashOfflineAtStartup" == eventMetaData) {
                disableAllDelivery(config)
            }
        }
    }

    override fun run() {
        super.run()

        if ("CrashOfflineAtStartup" == eventMetaData) {
            throw RuntimeException("Startup crash")
        }
    }
}
