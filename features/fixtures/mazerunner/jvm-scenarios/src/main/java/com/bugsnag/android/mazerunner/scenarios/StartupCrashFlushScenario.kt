package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.bugsnag.android.Configuration
import com.bugsnag.android.mazerunner.disableAllDelivery

/**
 * Generates an uncaught exception, catches it, and persists it to disc, preventing any delivery.
 *
 * To generate a startup crash, set "EVENT_METADATA" to "StartupCrash", otherwise the default
 * behaviour is to report a normal crash.
 *
 * The mazerunner scenario that tests this works in 3 parts:
 *
 * 1. Generate and persist a normal crash on disk, by waiting longer than the threshold for
 *    a startup crash
 * 2. Generate and persist a startup crash on disk, by crashing as soon as the scenario starts
 * 3. Send the stored reports. The startup crash should be delivered synchronously on the main thread,
 * and the normal crash asynchronously.
 */
internal class StartupCrashFlushScenario(
    config: Configuration,
    context: Context,
    eventMetadata: String
) : Scenario(config, context, eventMetadata) {

    init {
        if ("CrashOfflineWithDelay" == eventMetadata || "CrashOfflineAtStartup" == eventMetadata) {
            // Part 2 - Persist a startup crash to disk
            disableAllDelivery(config)
        }
    }

    override fun startScenario() {
        super.startScenario()
        if ("CrashOfflineWithDelay" == eventMetadata) {
            Handler(Looper.getMainLooper()).postDelayed(
                Runnable {
                    throw RuntimeException("Regular crash")
                },
                6000
            )
        } else if ("CrashOfflineAtStartup" == eventMetadata) {
            throw RuntimeException("Startup crash")
        }
    }
}
