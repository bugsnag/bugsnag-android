package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import android.os.Build
import android.os.StrictMode
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.BugsnagThreadViolationListener
import com.bugsnag.android.Configuration
import java.io.File

/**
 * Generates a strictmode exception caused by writing to disc on main thread
 */
internal class StrictModeDiscScenario(
    config: Configuration,
    context: Context,
    eventMetadata: String
) : Scenario(config, context, eventMetadata) {

    override fun startScenario() {
        super.startScenario()
        setupBugsnagStrictModeDetection()
        val file = File(context.cacheDir, "fake")
        file.writeBytes("test".toByteArray())
    }

    private fun setupBugsnagStrictModeDetection() {
        val policy = StrictMode.ThreadPolicy.Builder().detectDiskWrites()

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            policy.penaltyDeath()
        } else {
            val listener = BugsnagThreadViolationListener(Bugsnag.getClient())
            policy.penaltyListener(context.mainExecutor, listener)
        }
        StrictMode.setThreadPolicy(policy.build())
    }
}
