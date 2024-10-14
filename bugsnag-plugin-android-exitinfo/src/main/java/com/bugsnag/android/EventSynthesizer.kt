package com.bugsnag.android

import android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_CACHED
import android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_CANT_SAVE_STATE
import android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
import android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND_SERVICE
import android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_GONE
import android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_PERCEPTIBLE
import android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_PERCEPTIBLE_PRE_26
import android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_SERVICE
import android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_TOP_SLEEPING
import android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE
import android.app.ActivityManager.RunningAppProcessInfo.REASON_PROVIDER_IN_USE
import android.app.ActivityManager.RunningAppProcessInfo.REASON_SERVICE_IN_USE
import android.app.ApplicationExitInfo
import android.os.Build
import androidx.annotation.RequiresApi

@RequiresApi(Build.VERSION_CODES.R)
internal class EventSynthesizer(
    private val anrEventEnhancer: (Event, ApplicationExitInfo) -> Unit,
    private val exitInfoPluginStore: ExitInfoPluginStore
) {
    fun createEventWithAnrExitInfo(appExitInfo: ApplicationExitInfo): Event? {
        val (_, exitInfoKeys) = exitInfoPluginStore.load()
        val exitInfoKey = ExitInfoKey(appExitInfo)
        if (appExitInfo.reason == ApplicationExitInfo.REASON_ANR &&
            exitInfoKey !in exitInfoKeys
        ) {
            exitInfoPluginStore.addExitInfoKey(exitInfoKey)
            val newEvent = InternalHooks.createEmptyANR(exitInfoKey.timestamp)
            addExitInfoMetadata(newEvent, appExitInfo)
            anrEventEnhancer(newEvent, appExitInfo)
            val thread = newEvent.threads.find { it.name == "main" } ?: newEvent.threads.first()
            val error = newEvent.addError("ANR", appExitInfo.description)
            error.stacktrace.addAll(thread.stacktrace)
            return newEvent
        }
        return null
    }

    private fun addExitInfoMetadata(
        newEvent: Event,
        appExitInfo: ApplicationExitInfo
    ) {
        newEvent.addMetadata("exitinfo", "description", appExitInfo.description)
        newEvent.addMetadata(
            "exitinfo",
            "importance",
            getExitInfoImportance(appExitInfo.importance)
        )
        newEvent.addMetadata("exitinfo", "pss", appExitInfo.pss)
        newEvent.addMetadata("exitinfo", "rss", appExitInfo.rss)
    }

    private fun getExitInfoImportance(importance: Int): String {
        return when (importance) {
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
            else -> "unknown importance ($importance)"
        }
    }

    companion object {
        const val IMPORTANCE_EMPTY = 500
        const val IMPORTANCE_CANT_SAVE_STATE_PRE_26 = 170
        const val IMPORTANCE_TOP_SLEEPING_PRE_28 = 150
    }
}
