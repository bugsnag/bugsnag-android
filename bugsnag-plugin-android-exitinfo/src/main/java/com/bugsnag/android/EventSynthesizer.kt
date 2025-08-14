package com.bugsnag.android

import android.app.ApplicationExitInfo
import android.app.ApplicationExitInfo.REASON_ANR
import android.os.Build
import androidx.annotation.RequiresApi

private const val EXIT_INFO_METADATA = "exitInfo"

@RequiresApi(Build.VERSION_CODES.R)
internal class EventSynthesizer(
    private val createEmptyANRWithTimestamp: (Long) -> Event?,
    private val anrEventEnhancer: (Event, ApplicationExitInfo) -> Unit,
    private val exitInfoPluginStore: ExitInfoPluginStore,
    private val reportUnmatchedANRs: Boolean,
) {
    fun createEventWithExitInfo(appExitInfo: ApplicationExitInfo): Event? {
        // we only ever synthesize events for ANRs where the PID and the exit happened *after* the
        // previous run timestamp (which typically represents the last time the app was started)
        val previousRunTimestamp = exitInfoPluginStore.previousState?.timestamp ?: return null

        val exitInfoKey = ExitInfoKey(appExitInfo)
        if (exitInfoPluginStore.currentState.processedExitInfoKeys.contains(exitInfoKey)) {
            // this exit info has already been processed, so we skip it
            return null
        }

        // regardless of whether we create an event or not, we add the exit info key
        // to the store so that we don't process it again
        exitInfoPluginStore.addExitInfoKey(exitInfoKey)

        if (appExitInfo.pid == exitInfoPluginStore.previousState?.pid &&
            appExitInfo.timestamp > previousRunTimestamp &&
            appExitInfo.reason == REASON_ANR
        ) {
            return createEventWithUnmatchedANR(appExitInfo)
        }

        return null
    }

    private fun createEventWithUnmatchedANR(
        appExitInfo: ApplicationExitInfo
    ): Event? {
        if (!reportUnmatchedANRs) {
            return null
        }

        val newAnrEvent = createEmptyANRWithTimestamp(appExitInfo.timestamp)
            ?: return null
        addExitInfoMetadata(newAnrEvent, appExitInfo)
        anrEventEnhancer(newAnrEvent, appExitInfo)

        val thread = getErrorThread(newAnrEvent, appExitInfo.processName)
        val error = newAnrEvent.addError("ANR", appExitInfo.description)
        thread?.let { error.stacktrace.addAll(it.stacktrace) }

        return newAnrEvent
    }

    private fun getErrorThread(newNativeEvent: Event, processName: String): Thread? {
        val thread = newNativeEvent.threads.find { it.name == "main" }
            ?: newNativeEvent.threads.find { it.name == processName }
            ?: newNativeEvent.threads.firstOrNull()
        return thread
    }

    private fun addExitInfoMetadata(
        newEvent: Event,
        appExitInfo: ApplicationExitInfo
    ) {
        newEvent.addMetadata(EXIT_INFO_METADATA, "Description", appExitInfo.description)
        newEvent.addMetadata(
            EXIT_INFO_METADATA,
            "Importance",
            importanceDescriptionOf(appExitInfo)
        )

        val pss = appExitInfo.pss
        if (pss > 0) {
            newEvent.addMetadata(
                EXIT_INFO_METADATA, "Proportional Set Size (PSS)", "$pss kB"
            )
        }

        val rss = appExitInfo.rss
        if (rss > 0) {
            newEvent.addMetadata(
                EXIT_INFO_METADATA, "Resident Set Size (RSS)", "$rss kB"
            )
        }
    }
}
