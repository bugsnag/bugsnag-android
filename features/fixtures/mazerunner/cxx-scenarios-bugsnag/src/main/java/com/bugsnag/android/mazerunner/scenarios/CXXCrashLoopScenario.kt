package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration

/**
 * Triggers a crash loop which Bugsnag allows recovery from.
 */
internal class CXXCrashLoopScenario(
    config: Configuration,
    context: Context,
    eventMetadata: String?
) : Scenario(config, context, eventMetadata) {

    init {
        System.loadLibrary("cxx-scenarios-bugsnag")
        // Keep the launch window open to prevent the last run info file
        // from being corrupted by the app crashing while it's being written.
        config.launchDurationMillis = 0
    }

    external fun crash()

    override fun startScenario() {
        super.startScenario()
        val lastRunInfo = Bugsnag.getLastRunInfo()

        lastRunInfo?.let {
            Bugsnag.addMetadata("LastRunInfo", "crashed", it.crashed)
            Bugsnag.addMetadata("LastRunInfo", "crashedDuringLaunch", it.crashedDuringLaunch)
            Bugsnag.addMetadata(
                "LastRunInfo",
                "consecutiveLaunchCrashes",
                it.consecutiveLaunchCrashes
            )
        }

        // the last run info allows the scenario to escape from what would otherwise be
        // a crash loop, by conditionally entering a 'safe mode'.
        if (lastRunInfo?.crashed == true) {
            if (lastRunInfo.consecutiveLaunchCrashes < 2) {
                crash()
            } else {
                Bugsnag.notify(IllegalArgumentException("Safe mode enabled"))
            }
        } else {
            crash()
        }
    }
}
