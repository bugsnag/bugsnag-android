package com.bugsnag.android

import android.app.ActivityManager
import android.app.ApplicationExitInfo
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import com.bugsnag.android.ApplicationExitInfoMatcher.Companion.MATCH_ALL

@RequiresApi(Build.VERSION_CODES.R)
internal class ExitInfoCallback(
    private val context: Context,
    private val nativeEnhancer: (Event, ApplicationExitInfo) -> Unit,
    private val anrEventEnhancer: (Event, ApplicationExitInfo) -> Unit,
    private val exitInfoPluginStore: ExitInfoPluginStore?,
    private val applicationExitInfoMatcher: ApplicationExitInfoMatcher?,
) : OnSendCallback {

    override fun onSend(event: Event): Boolean {
        val am: ActivityManager =
            context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val allExitInfo: List<ApplicationExitInfo> =
            am.getHistoricalProcessExitReasons(context.packageName, MATCH_ALL, MAX_EXIT_INFO)
        val sessionIdBytes: ByteArray = event.session?.id?.toByteArray() ?: return true
        val exitInfo: ApplicationExitInfo =
            applicationExitInfoMatcher?.findExitInfoBySessionId(allExitInfo, sessionIdBytes)
                ?: applicationExitInfoMatcher?.findExitInfoByPid(allExitInfo) ?: return true
        exitInfoPluginStore?.addExitInfoKey(ExitInfoKey(exitInfo.pid, exitInfo.timestamp))

        try {
            val reason = exitReasonOf(exitInfo)
            event.addMetadata("app", "exitReason", reason)

            val importance = importanceDescriptionOf(exitInfo)
            event.addMetadata("app", "processImportance", importance)

            if (exitInfo.reason == ApplicationExitInfo.REASON_CRASH_NATIVE ||
                exitInfo.reason == ApplicationExitInfo.REASON_SIGNALED
            ) {
                nativeEnhancer(event, exitInfo)
                exitInfoPluginStore?.addExitInfoKey(ExitInfoKey(exitInfo))
            } else if (exitInfo.reason == ApplicationExitInfo.REASON_ANR) {
                anrEventEnhancer(event, exitInfo)
                exitInfoPluginStore?.addExitInfoKey(ExitInfoKey(exitInfo))
            }
        } catch (exc: Throwable) {
            return true
        }
        return true
    }

    internal companion object {
        private const val MAX_EXIT_INFO = 100
    }
}
