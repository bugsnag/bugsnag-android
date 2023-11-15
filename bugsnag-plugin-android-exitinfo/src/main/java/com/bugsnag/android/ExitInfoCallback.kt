package com.bugsnag.android

import android.app.ActivityManager
import android.app.ApplicationExitInfo
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi

@RequiresApi(Build.VERSION_CODES.R)
internal class ExitInfoCallback(
    private val context: Context,
    private val pid: Int?,
    private val nativeEnhancer: (Event, ApplicationExitInfo) -> Unit,
    private val anrEventEnhancer: (Event, ApplicationExitInfo) -> Unit
) : OnSendCallback {

    override fun onSend(event: Event): Boolean {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val allExitInfo = am.getHistoricalProcessExitReasons(context.packageName, 0, MAX_EXIT_INFO)
        val sessionIdBytes = event.session?.id?.toByteArray() ?: return true
        val exitInfo = findExitInfoBySessionId(allExitInfo, sessionIdBytes)
            ?: findExitInfoByPid(allExitInfo) ?: return true

        try {
            if (exitInfo.reason == ApplicationExitInfo.REASON_CRASH_NATIVE ||
                exitInfo.reason == ApplicationExitInfo.REASON_SIGNALED
            ) {
                nativeEnhancer(event, exitInfo)
            } else if (exitInfo.reason == ApplicationExitInfo.REASON_ANR) {
                anrEventEnhancer(event, exitInfo)
            }
        } catch (exc: Throwable) {
            return true
        }
        return true
    }

    private fun findExitInfoBySessionId(
        allExitInfo: List<ApplicationExitInfo>,
        sessionIdBytes: ByteArray
    ) = allExitInfo.find {
        it.processStateSummary?.contentEquals(sessionIdBytes) == true
    }

    private fun findExitInfoByPid(allExitInfo: List<ApplicationExitInfo>) =
        allExitInfo.find { it.pid == pid }

    internal companion object {
        const val MAX_EXIT_INFO = 100
    }
}
