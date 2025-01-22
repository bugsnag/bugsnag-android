package com.bugsnag.android

import android.app.ActivityManager
import android.app.ApplicationExitInfo
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi

@RequiresApi(Build.VERSION_CODES.R)
internal class ApplicationExitInfoMatcher(
    private val context: Context,
    private val pid: Int
) {
    fun matchExitInfo(event: Event): ApplicationExitInfo? {
        val am: ActivityManager =
            context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val allExitInfo: List<ApplicationExitInfo> =
            am.getHistoricalProcessExitReasons(context.packageName, MATCH_ALL, MAX_EXIT_INFO)
        val sessionIdBytes: ByteArray =
            event.session?.id?.toByteArray() ?: return null
        val exitInfo: ApplicationExitInfo =
            findExitInfoBySessionId(allExitInfo, sessionIdBytes)
                ?: findExitInfoByPid(allExitInfo) ?: return null
        return exitInfo
    }

    internal fun findExitInfoBySessionId(
        allExitInfo: List<ApplicationExitInfo>,
        sessionIdBytes: ByteArray
    ) = allExitInfo.find {
        it.processStateSummary?.contentEquals(sessionIdBytes) == true
    }

    internal fun findExitInfoByPid(allExitInfo: List<ApplicationExitInfo>) =
        allExitInfo.find { it.pid == pid }

    internal companion object {
        const val MATCH_ALL = 0
        const val MAX_EXIT_INFO = 100
    }
}
