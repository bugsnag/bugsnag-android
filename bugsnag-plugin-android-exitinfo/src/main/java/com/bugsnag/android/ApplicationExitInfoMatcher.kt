package com.bugsnag.android

import android.app.ApplicationExitInfo
import android.os.Build
import androidx.annotation.RequiresApi

@RequiresApi(Build.VERSION_CODES.R)
internal class ApplicationExitInfoMatcher(
    private val applicationExitInfo: List<ApplicationExitInfo>,
    private val previousState: ExitInfoPluginStore.PersistentState? = null,
) {
    fun matchExitInfo(event: Event): ApplicationExitInfo? {
        val sessionIdBytes: ByteArray = event.session?.id?.toByteArray() ?: return null
        return findExitInfoBySessionId(applicationExitInfo, sessionIdBytes)
            ?: findExitInfoByPid(applicationExitInfo)
    }

    internal fun findExitInfoBySessionId(
        allExitInfo: List<ApplicationExitInfo>,
        sessionIdBytes: ByteArray
    ) = allExitInfo.find {
        it.processStateSummary?.contentEquals(sessionIdBytes) == true
    }

    internal fun findExitInfoByPid(allExitInfo: List<ApplicationExitInfo>) =
        allExitInfo.find { it.pid == previousState?.pid }

    internal companion object {
        const val MATCH_ALL = 0
        const val MAX_EXIT_INFO = 100
    }
}
