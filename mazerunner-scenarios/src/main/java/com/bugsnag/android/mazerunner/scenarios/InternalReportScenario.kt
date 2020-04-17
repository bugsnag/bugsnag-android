package com.bugsnag.android.mazerunner.scenarios

import android.app.Activity
import android.content.Context
import android.os.Build
import android.os.storage.StorageManager

import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration
import java.io.File

/**
 * Sends a handled exception to Bugsnag, which does not include session data.
 */
internal class InternalReportScenario(config: Configuration,
                                      context: Context) : Scenario(config, context) {

    init {
        config.autoTrackSessions = false

        if (context is Activity) {
            eventMetaData = context.intent.getStringExtra("EVENT_METADATA")
            val errDir = File(context.cacheDir, "bugsnag-errors")

            if (eventMetaData == "tombstone" && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                errDir.mkdir()
                val storageManager = context.getSystemService(Context.STORAGE_SERVICE) as StorageManager
                storageManager.setCacheBehaviorGroup(errDir, true)
                storageManager.setCacheBehaviorTombstone(errDir, true)
            }

            if (eventMetaData != "non-crashy") {
                disableAllDelivery(config)
            } else {
                val files = errDir.listFiles()
                files.forEach { it.writeText("{[]}") }
            }
        }
    }

    override fun run() {
        super.run()

        if (eventMetaData != "non-crashy") {
            Bugsnag.notify(java.lang.RuntimeException("Whoops"))
        }
    }
}
