package com.bugsnag.android

import android.app.ApplicationExitInfo
import android.os.Build
import androidx.annotation.RequiresApi
import com.bugsnag.android.Thread as BugsnagThread

internal class TombstoneEnhancer(
    private val logger: Logger
) : (Event, ApplicationExitInfo) -> Unit {
    @RequiresApi(Build.VERSION_CODES.R)
    override fun invoke(event: Event, exitInfo: ApplicationExitInfo) {
        try {
            TombstoneParser(logger).parse(exitInfo) { thread ->
                mergeThreadIntoEvent(
                    thread,
                    event
                )
            }
        } catch (ex: Exception) {
            logger.w("could not parse tombstone file", ex)
        }
    }

    private fun mergeThreadIntoEvent(newThread: BugsnagThread, event: Event) {
        val existingThread = event.threads.find { currentThread ->
            currentThread.id == newThread.id
        }
        if (existingThread != null) {
            existingThread.stacktrace.clear()
            newThread.stacktrace.forEach { stackFrame ->
                existingThread.stacktrace.add(stackFrame)
            }

        } else {
            event.threads.add(newThread)
        }
    }
}