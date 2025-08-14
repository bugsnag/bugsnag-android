package com.bugsnag.android

import android.annotation.SuppressLint
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
import android.app.ApplicationExitInfo.REASON_ANR
import android.app.ApplicationExitInfo.REASON_CRASH
import android.app.ApplicationExitInfo.REASON_CRASH_NATIVE
import android.app.ApplicationExitInfo.REASON_DEPENDENCY_DIED
import android.app.ApplicationExitInfo.REASON_EXCESSIVE_RESOURCE_USAGE
import android.app.ApplicationExitInfo.REASON_EXIT_SELF
import android.app.ApplicationExitInfo.REASON_FREEZER
import android.app.ApplicationExitInfo.REASON_INITIALIZATION_FAILURE
import android.app.ApplicationExitInfo.REASON_LOW_MEMORY
import android.app.ApplicationExitInfo.REASON_OTHER
import android.app.ApplicationExitInfo.REASON_PACKAGE_STATE_CHANGE
import android.app.ApplicationExitInfo.REASON_PACKAGE_UPDATED
import android.app.ApplicationExitInfo.REASON_PERMISSION_CHANGE
import android.app.ApplicationExitInfo.REASON_SIGNALED
import android.app.ApplicationExitInfo.REASON_USER_REQUESTED
import android.app.ApplicationExitInfo.REASON_USER_STOPPED
import android.os.Build
import androidx.annotation.RequiresApi

private const val IMPORTANCE_CANT_SAVE_STATE_PRE_26 = 170

@RequiresApi(Build.VERSION_CODES.R)
internal fun exitReasonOf(exitInfo: ApplicationExitInfo) =
    when (exitInfo.reason) {
        ApplicationExitInfo.REASON_UNKNOWN -> "unknown reason (${exitInfo.reason})"
        REASON_EXIT_SELF -> "exit self"
        REASON_SIGNALED -> "signaled"
        REASON_LOW_MEMORY -> "low memory"
        REASON_CRASH -> "crash"
        REASON_CRASH_NATIVE -> "crash native"
        REASON_ANR -> "ANR"
        REASON_INITIALIZATION_FAILURE -> "initialization failure"
        REASON_PERMISSION_CHANGE -> "permission change"
        REASON_EXCESSIVE_RESOURCE_USAGE -> "excessive resource usage"
        REASON_USER_REQUESTED -> "user requested"
        REASON_USER_STOPPED -> "user stopped"
        REASON_DEPENDENCY_DIED -> "dependency died"
        REASON_OTHER -> "other"
        REASON_FREEZER -> "freezer"
        REASON_PACKAGE_STATE_CHANGE -> "package state change"
        REASON_PACKAGE_UPDATED -> "package updated"
        else -> "unknown reason (${exitInfo.reason})"
    }

@RequiresApi(Build.VERSION_CODES.R)
@SuppressLint("SwitchIntDef")
@Suppress("DEPRECATION")
internal fun importanceDescriptionOf(exitInfo: ApplicationExitInfo) =
    when (exitInfo.importance) {
        IMPORTANCE_FOREGROUND -> "foreground"
        IMPORTANCE_FOREGROUND_SERVICE -> "foreground service"
        IMPORTANCE_TOP_SLEEPING -> "top sleeping"
        IMPORTANCE_TOP_SLEEPING_PRE_28 -> "top sleeping"
        IMPORTANCE_VISIBLE -> "visible"
        IMPORTANCE_PERCEPTIBLE -> "perceptible"
        IMPORTANCE_PERCEPTIBLE_PRE_26 -> "perceptible"
        IMPORTANCE_CANT_SAVE_STATE, IMPORTANCE_CANT_SAVE_STATE_PRE_26 -> "can't save state"
        IMPORTANCE_SERVICE -> "service"
        IMPORTANCE_CACHED -> "cached/background"
        IMPORTANCE_GONE -> "gone"
        IMPORTANCE_EMPTY -> "empty"
        REASON_PROVIDER_IN_USE -> "provider in use"
        REASON_SERVICE_IN_USE -> "service in use"
        else -> "unknown importance (${exitInfo.importance})"
    }
