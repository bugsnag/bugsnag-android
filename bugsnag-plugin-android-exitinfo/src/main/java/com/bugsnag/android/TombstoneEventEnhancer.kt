package com.bugsnag.android

import android.app.ApplicationExitInfo
import android.os.Build
import androidx.annotation.RequiresApi
import com.bugsnag.android.Thread as BugsnagThread

internal class TombstoneEventEnhancer(
    private val logger: Logger,
    private val listOpenFds: Boolean,
    private val includeLogcat: Boolean
) : (Event, ApplicationExitInfo) -> Unit {
    @RequiresApi(Build.VERSION_CODES.R)
    override fun invoke(event: Event, exitInfo: ApplicationExitInfo) {
        try {
            TombstoneParser(logger).parse(
                exitInfo,
                listOpenFds,
                includeLogcat,
                threadConsumer = { thread ->
                    mergeThreadIntoEvent(
                        thread,
                        event
                    )
                },
                { fd, path, owner ->
                    val fdInfo = if (owner.isNotEmpty()) mapOf(
                        "path" to path,
                        "owner" to owner
                    ) else mapOf("path" to path)
                    event.addMetadata("Open FileDescriptors", fd.toString(), fdInfo)
                },
                logcatConsumer = { log ->
                    event.addMetadata("Log Messages", "Log Messages", log)
                }
            )
        } catch (ex: Exception) {
            logger.w("could not parse tombstone file", ex)
        }
    }

    private fun mergeThreadIntoEvent(newThread: BugsnagThread, event: Event) {
        val existingThread = event.threads.find { currentThread ->
            currentThread.id == newThread.id
        }
        if (existingThread != null) {
            existingThread.name = newThread.name
            existingThread.stacktrace.clear()
            existingThread.stacktrace.addAll(newThread.stacktrace)
        } else {
            event.threads.add(newThread)
        }
    }
}
