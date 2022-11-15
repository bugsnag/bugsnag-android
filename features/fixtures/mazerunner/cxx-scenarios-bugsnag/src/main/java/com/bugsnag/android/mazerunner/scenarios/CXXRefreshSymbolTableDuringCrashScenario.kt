package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.Configuration
import com.bugsnag.android.ndk.BugsnagNDK
import kotlin.concurrent.thread

class CXXRefreshSymbolTableDuringCrashScenario(
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

    external fun activate(): Int

    override fun startScenario() {
        super.startScenario()

        thread {
            while (true) {
                BugsnagNDK.refreshSymbolTable()
            }
        }

        Thread.sleep(1L)
        activate()
    }
}
