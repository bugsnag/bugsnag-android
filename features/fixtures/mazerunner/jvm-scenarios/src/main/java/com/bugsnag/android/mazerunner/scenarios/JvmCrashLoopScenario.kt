package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration
import com.bugsnag.android.OnErrorCallback

/**
 * Triggers a crash loop which Bugsnag allows recovery from.
 */
internal class JvmCrashLoopScenario(
    config: Configuration,
    context: Context,
    eventMetadata: String?
) : Scenario(config, context, eventMetadata) {

    init {
        config.addOnError(
            OnErrorCallback { event ->
                Bugsnag.getLastRunInfo()?.let {
                    event.addMetadata("LastRunInfo", "crashed", it.crashed)
                    event.addMetadata("LastRunInfo", "crashedDuringLaunch", it.crashedDuringLaunch)
                    event.addMetadata(
                        "LastRunInfo",
                        "consecutiveLaunchCrashes",
                        it.consecutiveLaunchCrashes
                    )
                }
                true
            }
        )
    }

    @Suppress("UseCheckOrError")
    override fun startScenario() {
        super.startScenario()
        val lastRunInfo = Bugsnag.getLastRunInfo()

        // the last run info allows the scenario to escape from what would otherwise be
        // a crash loop, by conditionally entering a 'safe mode'.
        if (lastRunInfo?.crashed == true) {
            if (lastRunInfo.consecutiveLaunchCrashes < 2) {
                throw IllegalStateException("Second JVM crash")
            } else {
                Bugsnag.notify(IllegalArgumentException("Safe mode enabled"))
            }
        } else {
            throw IllegalStateException("First JVM crash")
        }
    }
}
