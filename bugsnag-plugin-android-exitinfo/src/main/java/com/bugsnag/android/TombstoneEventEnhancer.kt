package com.bugsnag.android

import android.app.ApplicationExitInfo
import android.os.Build
import androidx.annotation.RequiresApi
import com.bugsnag.android.Thread as BugsnagThread

@RequiresApi(Build.VERSION_CODES.R)
internal class TombstoneEventEnhancer(
    private val logger: Logger,
    listOpenFds: Boolean,
    includeLogcat: Boolean
) : (Event, ApplicationExitInfo) -> Unit {
    private val tombstoneParser = TombstoneParser(logger, listOpenFds, includeLogcat)

    override fun invoke(event: Event, exitInfo: ApplicationExitInfo) {
        try {
            val inputStream = exitInfo.traceInputStream ?: return
            tombstoneParser.parse(
                inputStream,
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
                },
                { abortMessage ->
                    val error = event.errors.find { it.errorClass == "SIGABRT" }
                        ?: event.errors.firstOrNull()

                    error?.errorMessage = abortMessage
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
