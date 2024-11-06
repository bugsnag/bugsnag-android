package com.bugsnag.android.mazerunner

import android.app.Application
import android.os.Build
import android.os.StrictMode
import com.bugsnag.android.performance.BugsnagPerformance
import com.bugsnag.android.performance.PerformanceConfiguration
import com.bugsnag.android.performance.internal.InternalDebug

class MazerunnerApp : Application() {

    override fun onCreate() {
        super.onCreate()
        triggerStartupAnrIfRequired()
        setupNonSdkUsageStrictMode()
        triggerManualSessionIfRequired()
        InternalDebug.spanBatchSizeSendTriggerPoint = 1
        BugsnagPerformance.start(PerformanceConfiguration.load(this))
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
                .penaltyLog()
                .build()
            StrictMode.setVmPolicy(policy)
        }
    }
}
