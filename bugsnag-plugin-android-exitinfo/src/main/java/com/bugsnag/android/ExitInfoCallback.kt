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
            val reason = exitReasonOf(exitInfo)
            event.addMetadata("app", "exitReason", reason)

            val importance = importanceDescriptionOf(exitInfo)
            event.addMetadata("app", "processImportance", importance)

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

    private fun exitReasonOf(exitInfo: ApplicationExitInfo) = when (exitInfo.reason) {
        ApplicationExitInfo.REASON_UNKNOWN -> "unknown reason (${exitInfo.reason})"
        ApplicationExitInfo.REASON_EXIT_SELF -> "exit self"
        ApplicationExitInfo.REASON_SIGNALED -> "signaled"
        ApplicationExitInfo.REASON_LOW_MEMORY -> "low memory"
        ApplicationExitInfo.REASON_CRASH -> "crash"
        ApplicationExitInfo.REASON_CRASH_NATIVE -> "crash native"
        ApplicationExitInfo.REASON_ANR -> "ANR"
        ApplicationExitInfo.REASON_INITIALIZATION_FAILURE -> "initialization failure"
        ApplicationExitInfo.REASON_PERMISSION_CHANGE -> "permission change"
        ApplicationExitInfo.REASON_EXCESSIVE_RESOURCE_USAGE -> "excessive resource usage"
        ApplicationExitInfo.REASON_USER_REQUESTED -> "user requested"
        ApplicationExitInfo.REASON_USER_STOPPED -> "user stopped"
        ApplicationExitInfo.REASON_DEPENDENCY_DIED -> "dependency died"
        ApplicationExitInfo.REASON_OTHER -> "other"
        ApplicationExitInfo.REASON_FREEZER -> "freezer"
        ApplicationExitInfo.REASON_PACKAGE_STATE_CHANGE -> "package state change"
        ApplicationExitInfo.REASON_PACKAGE_UPDATED -> "package updated"
        else -> "unknown reason (${exitInfo.reason})"
    }

    private fun importanceDescriptionOf(exitInfo: ApplicationExitInfo) = when (exitInfo.importance) {
        ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND -> "foreground"
        ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND_SERVICE -> "foreground service"
        ActivityManager.RunningAppProcessInfo.IMPORTANCE_TOP_SLEEPING -> "top sleeping"
        ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE -> "visible"
        ActivityManager.RunningAppProcessInfo.IMPORTANCE_PERCEPTIBLE -> "perceptible"
        ActivityManager.RunningAppProcessInfo.IMPORTANCE_CANT_SAVE_STATE -> "can't save state"
        ActivityManager.RunningAppProcessInfo.IMPORTANCE_SERVICE -> "service"
        ActivityManager.RunningAppProcessInfo.IMPORTANCE_CACHED -> "cached"
        ActivityManager.RunningAppProcessInfo.IMPORTANCE_GONE -> "gone"
        else -> "unknown importance (${exitInfo.importance})"
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
