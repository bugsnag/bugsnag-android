package com.bugsnag.android

import android.app.ApplicationExitInfo
import android.app.ApplicationExitInfo.REASON_ANR
import android.os.Build
import androidx.annotation.RequiresApi

@RequiresApi(Build.VERSION_CODES.R)
internal class EventSynthesizer(
    private val anrEventEnhancer: (Event, ApplicationExitInfo) -> Unit,
    private val exitInfoPluginStore: ExitInfoPluginStore,
    private val reportUnmatchedANRs: Boolean,
) {
    fun createEventWithExitInfo(appExitInfo: ApplicationExitInfo): Event? {
        val knownExitInfoKeys = exitInfoPluginStore.exitInfoKeys
        val exitInfoKey = ExitInfoKey(appExitInfo)

        if (knownExitInfoKeys.contains(exitInfoKey)) {
            return null
        }

        exitInfoPluginStore.addExitInfoKey(exitInfoKey)

        return when (appExitInfo.reason) {
            REASON_ANR -> {
                createEventWithUnmatchedANR(exitInfoKey, appExitInfo)
            }

            else -> null
        }
    }

    private fun createEventWithUnmatchedANR(
        exitInfoKey: ExitInfoKey,
        appExitInfo: ApplicationExitInfo
    ): Event? {
        if (reportUnmatchedANRs) {
            val newAnrEvent = InternalHooks.createEmptyANR(exitInfoKey.timestamp)
                ?: return null
            addExitInfoMetadata(newAnrEvent, appExitInfo)
            anrEventEnhancer(newAnrEvent, appExitInfo)
            val thread = getErrorThread(newAnrEvent)
            val error = newAnrEvent.addError("ANR", appExitInfo.description)
            thread?.let { error.stacktrace.addAll(it.stacktrace) }

            return newAnrEvent
        } else {
            return null
        }
    }

    private fun getErrorThread(newNativeEvent: Event): Thread? {
        val thread = newNativeEvent.threads.find { it.name == "main" }
            ?: newNativeEvent.threads.firstOrNull()
        return thread
    }

    private fun addExitInfoMetadata(
        newEvent: Event,
        appExitInfo: ApplicationExitInfo
    ) {
        newEvent.addMetadata("exitInfo", "Description", appExitInfo.description)
        newEvent.addMetadata(
            "exitInfo",
            "Importance",
            importanceDescriptionOf(appExitInfo)
        )

        val pss = appExitInfo.pss
        if (pss > 0) {
            newEvent.addMetadata(
                "exitInfo", "Proportional Set Size (PSS)", "$pss kB"
            )
        }

        val rss = appExitInfo.rss
        if (rss > 0) {
            newEvent.addMetadata(
                "exitInfo", "Resident Set Size (RSS)", "$rss kB"
            )
        }
    }
}
