package com.bugsnag.android.mazerunner.scenarios

import android.content.Context

import com.bugsnag.android.Configuration
import java.io.File

internal class EmptyCacheFileScenario(config: Configuration,
                                      context: Context) : Scenario(config, context) {

    init {
        config.autoTrackSessions = false
    }

    override fun run() {
        super.run()

        if (eventMetaData != "online") {
            // create an empty file
            val dir = File(context.cacheDir, "bugsnag-errors")
            val file = File(dir, "1504255147933_30b7e350-dcd1-4032-969e-98d30be62bbc.json")
            file.createNewFile()
        }
    }
}
