package com.bugsnag.android

import android.app.ActivityManager
import android.app.ApplicationExitInfo
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi

internal class ExitInfoCallback(
    private val context: Context,
    private val nativeEnhancer: (Event, ApplicationExitInfo) -> Unit,
    private val anrEventEnhancer: (Event, ApplicationExitInfo) -> Unit
) : OnSendCallback {

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onSend(event: Event): Boolean {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val allExitInfo = am.getHistoricalProcessExitReasons(context.packageName, 0, MAX_EXIT_INFO)
        val sessionIdBytes = event.session?.id?.toByteArray() ?: return true
        val exitInfo =
            allExitInfo.find { it.processStateSummary?.contentEquals(sessionIdBytes) == true }
                ?: return true

        try {
            if (exitInfo.reason == ApplicationExitInfo.REASON_CRASH_NATIVE) {
                nativeEnhancer(event, exitInfo)
            } else if (exitInfo.reason == ApplicationExitInfo.REASON_ANR) {
                anrEventEnhancer(event, exitInfo)
            }
        } catch (exc: Throwable) {
            return true
        }
        return true
    }

    internal companion object {
        const val MAX_EXIT_INFO = 100
    }
}
