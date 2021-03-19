package com.bugsnag.android.mazerunner

import android.app.Application
import android.os.Build
import android.os.StrictMode

class MazerunnerApp : Application() {

    override fun onCreate() {
        super.onCreate()
        setupNonSdkUsageStrictMode()
    }

    /**
     * Configures the mazerunner app so that it will terminate with an exception if [StrictMode]
     * detects that non-public Android APIs have been used. This is intended to provide an
     * early warning system if Bugsnag is using these features internally.
     *
     * https://developer.android.com/about/versions/11/behavior-changes-all#non-sdk-restrictions
     */
    private fun setupNonSdkUsageStrictMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val policy = StrictMode.VmPolicy.Builder()
                .detectNonSdkApiUsage()
                .penaltyDeath()
                .build()
            StrictMode.setVmPolicy(policy)
        }
    }
}
