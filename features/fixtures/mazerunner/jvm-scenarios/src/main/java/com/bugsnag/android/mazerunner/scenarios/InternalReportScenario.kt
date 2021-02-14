package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import android.os.Build
import android.os.storage.StorageManager
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration
import java.io.File

/**
 * Sends a handled exception to Bugsnag, which does not include session data.
 */
internal class InternalReportScenario(
    config: Configuration,
    context: Context,
    eventMetadata: String
) : Scenario(config, context, eventMetadata) {

    init {
        config.autoTrackSessions = false

        val errDir = File(context.cacheDir, "bugsnag-errors")

        if (eventMetadata == "tombstone" && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            errDir.mkdir()
            val storageManager = context.getSystemService(Context.STORAGE_SERVICE) as StorageManager
            storageManager.setCacheBehaviorGroup(errDir, true)
            storageManager.setCacheBehaviorTombstone(errDir, true)
        }
    }

    override fun startScenario() {
        super.startScenario()

        disableAllDelivery(config)
        Bugsnag.notify(java.lang.RuntimeException("Whoops"))
    }
}
