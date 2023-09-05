package com.bugsnag.android

import android.app.ApplicationExitInfo
import android.os.Build
import androidx.annotation.RequiresApi
import com.bugsnag.android.Thread as BugsnagThread

internal class TraceEventEnhancer(
    private val logger: Logger,
    private val projectPackages: Collection<String>,
) : (Event, ApplicationExitInfo) -> Unit {
    @RequiresApi(Build.VERSION_CODES.R)
    override fun invoke(event: Event, exitInfo: ApplicationExitInfo) {
        try {
            exitInfo.traceInputStream?.use { inputStream ->
                val parser = TraceParser(logger, projectPackages)
                val newThreads = mutableListOf<BugsnagThread>()
                parser.parse(inputStream) { thread -> newThreads.add(thread) }

                // replace all of the existing threads with the new list from the trace file
                event.threads.clear()
                event.threads.addAll(newThreads)
            }
        } catch (ex: Exception) {
            logger.w("could not parse trace file", ex)
        }
    }
}
