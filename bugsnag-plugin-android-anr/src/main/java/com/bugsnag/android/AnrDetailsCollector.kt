package com.bugsnag.android

import android.app.ActivityManager
import android.app.ActivityManager.ProcessErrorStateInfo
import android.content.Context
import android.os.Process
import android.support.annotation.VisibleForTesting

internal class AnrDetailsCollector {

    fun collectAnrDetails(ctx: Context): ProcessErrorStateInfo? {
        val am = ctx.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        return captureProcessErrorState(am, Process.myPid())
    }

    /**
     * Collects information about an ANR, by querying an activity manager for information about
     * any proceses which are currently in an error condition.
     *
     * See https://developer.android.com/reference/android/app/ActivityManager.html#getProcessesInErrorState()
     */
    @VisibleForTesting
    internal fun captureProcessErrorState(am: ActivityManager, pid: Int): ProcessErrorStateInfo? {
        return try {
            val processes = am.processesInErrorState ?: emptyList()
            processes.firstOrNull { it.pid == pid }
        } catch (exc: RuntimeException) {
            null
        }
    }

    internal fun mutateError(error: Error, anrState: ProcessErrorStateInfo) {
        // TODO truncate these values?
        error.exceptionMessage = anrState.shortMsg
    }
}
