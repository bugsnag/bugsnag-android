package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration

class ExitInfoScenario(
    config: com.bugsnag.android.Configuration,
    context: android.content.Context,
    eventMetadata: String?
) : Scenario(config, context, eventMetadata) {
    external fun crash(value: Int): Int
    override fun startScenario() {
        super.startScenario()
        Bugsnag.startSession()
        val main: android.os.Handler = android.os.Handler(android.os.Looper.getMainLooper())
        main.postDelayed(object : java.lang.Runnable {
            override fun run() {
                crash(2726)
            }
        }, 500)
    }
}