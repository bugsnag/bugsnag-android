package com.bugsnag.android.mazerunner.scenarios

import android.content.Context
import android.os.StrictMode
import com.bugsnag.android.Configuration
import java.io.File

/**
 * Generates a strictmode exception caused by writing to disc on main thread
 */
internal class StrictModeDiscScenario(config: Configuration,
                                      context: Context) : Scenario(config, context) {

    override fun run() {
        super.run()
        StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.Builder()
            .detectDiskWrites()
            .penaltyDeath()
            .build())
        val file = File(context.cacheDir, "fake")
        file.writeBytes("test".toByteArray())
    }

}
