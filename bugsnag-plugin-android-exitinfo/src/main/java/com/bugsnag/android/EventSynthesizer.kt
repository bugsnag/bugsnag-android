package com.bugsnag.android

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
            val newEvent = NativeInterface.createEmptyEvent()
            anrEventEnhancer(newEvent, appExitInfo)
            val thread = newEvent.threads.find { it.name == "main" } ?: newEvent.threads.first()
            val error = newEvent.addError("ANR", appExitInfo.description)
            error.stacktrace.addAll(thread.stacktrace)
            return newEvent
        }
        return null
    }
}
