package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration
import com.bugsnag.android.mazerunner.log

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
    }

    external fun crash()

    override fun startBugsnag(startBugsnagOnly: Boolean) {
        config.launchDurationMillis = 0
        super.startBugsnag(startBugsnagOnly)
    }

    override fun startScenario() {
        super.startScenario()
        val lastRunInfo = Bugsnag.getLastRunInfo()

        lastRunInfo?.let {
            log("CXXCrashLoopScenario: loaded LastRunInfo: $it")
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
            log("Confirmed last launch crashed.")
            if (lastRunInfo.consecutiveLaunchCrashes < 2) {
                log("CXXCrashLoopScenario: <2 previous crashes, crashing again")
                crash()
            } else {
                log("CXXCrashLoopScenario: >=2 previous crashes, enabling safe mode")
                Bugsnag.notify(IllegalArgumentException("Safe mode enabled"))
            }
        } else {
            log("CXXCrashLoopScenario: no previous crashes, about to crash")
            crash()
        }
    }
}
