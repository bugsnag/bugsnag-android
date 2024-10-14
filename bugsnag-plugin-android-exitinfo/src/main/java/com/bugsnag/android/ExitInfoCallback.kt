package com.bugsnag.android

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_CACHED
import android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_CANT_SAVE_STATE
import android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_EMPTY
import android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
import android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND_SERVICE
import android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_GONE
import android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_PERCEPTIBLE
import android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_PERCEPTIBLE_PRE_26
import android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_SERVICE
import android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_TOP_SLEEPING
import android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_TOP_SLEEPING_PRE_28
import android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE
import android.app.ActivityManager.RunningAppProcessInfo.REASON_PROVIDER_IN_USE
import android.app.ActivityManager.RunningAppProcessInfo.REASON_SERVICE_IN_USE
import android.app.ApplicationExitInfo
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.VisibleForTesting

@RequiresApi(Build.VERSION_CODES.R)
@VisibleForTesting
internal class ExitInfoCallback(
    private val context: Context,
    private val pid: Int?,
    private val nativeEnhancer: (Event, ApplicationExitInfo) -> Unit,
    private val anrEventEnhancer: (Event, ApplicationExitInfo) -> Unit,
    private val exitInfoPluginStore: ExitInfoPluginStore?
) : OnSendCallback {

    override fun onSend(event: Event): Boolean {
        val am: ActivityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val allExitInfo: List<ApplicationExitInfo> = am.getHistoricalProcessExitReasons(context.packageName, 0, MAX_EXIT_INFO)
        val sessionIdBytes: ByteArray = event.session?.id?.toByteArray() ?: return true
        val exitInfo: ApplicationExitInfo = findExitInfoBySessionId(allExitInfo, sessionIdBytes)
            ?: findExitInfoByPid(allExitInfo) ?: return true
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

    @SuppressLint("SwitchIntDef")
    @Suppress("DEPRECATION")
    private fun importanceDescriptionOf(exitInfo: ApplicationExitInfo) = when (exitInfo.importance) {
        IMPORTANCE_FOREGROUND -> "foreground"
        IMPORTANCE_FOREGROUND_SERVICE -> "foreground service"
        IMPORTANCE_TOP_SLEEPING -> "top sleeping"
        IMPORTANCE_TOP_SLEEPING_PRE_28 -> "top sleeping"
        IMPORTANCE_VISIBLE -> "visible"
        IMPORTANCE_PERCEPTIBLE -> "perceptible"
        IMPORTANCE_PERCEPTIBLE_PRE_26 -> "perceptible"
        IMPORTANCE_CANT_SAVE_STATE -> "can't save state"
        IMPORTANCE_CANT_SAVE_STATE_PRE_26 -> "can't save state"
        IMPORTANCE_SERVICE -> "service"
        IMPORTANCE_CACHED -> "cached/background"
        IMPORTANCE_GONE -> "gone"
        IMPORTANCE_EMPTY -> "empty"
        REASON_PROVIDER_IN_USE -> "provider in use"
        REASON_SERVICE_IN_USE -> "service in use"
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
        private const val MAX_EXIT_INFO = 100
        private const val IMPORTANCE_CANT_SAVE_STATE_PRE_26 = 170
    }
}
