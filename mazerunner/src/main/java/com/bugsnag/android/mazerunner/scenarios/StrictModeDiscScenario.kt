package com.bugsnag.android.mazerunner.scenarios

import android.os.StrictMode
import java.io.File

/**
 * Generates a strictmode exception caused by writing to disc on main thread
 */
internal class StrictModeDiscScenario : Scenario() {

    override fun run() {
        StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.Builder()
            .detectDiskWrites()
            .penaltyDeath()
            .build())
        val file = File(context?.cacheDir, "fake")
        file.writeBytes("test".toByteArray())
    }

}
