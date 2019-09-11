package com.bugsnag.android.mazerunner.scenarios

import android.app.Activity
import android.content.Context
import android.os.Build
import android.os.storage.StorageManager

import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration
import java.io.File

internal class CacheTombstoneScenario(config: Configuration,
                                      context: Context) : Scenario(config, context) {

    init {
        config.setAutoCaptureSessions(false)
    }

    override fun run() {
        super.run()

        if (eventMetaData != "online") {
            // create an empty file
            val dir = File(context.cacheDir, "bugsnag-errors")
            val file = File(dir, "1504255147933_30b7e350-dcd1-4032-969e-98d30be62bbc.json")
            file.createNewFile()

            // set tombstone cache behavior on the entire directory
            val storageManager = context.getSystemService(Context.STORAGE_SERVICE) as StorageManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                storageManager.setCacheBehaviorTombstone(dir, true)
            }
        }
    }
}
