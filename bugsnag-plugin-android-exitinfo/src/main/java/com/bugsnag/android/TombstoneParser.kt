package com.bugsnag.android

import android.app.ApplicationExitInfo
import android.os.Build
import androidx.annotation.RequiresApi
import com.bugsnag.android.Thread.State
import com.bugsnag.android.repackaged.server.os.TombstoneProtos
import com.bugsnag.android.repackaged.server.os.TombstoneProtos.Tombstone
import com.bugsnag.android.Thread as BugsnagThread

internal class TombstoneParser(
    private val logger: Logger
) {

    @RequiresApi(Build.VERSION_CODES.R)
    fun parse(
        exitInfo: ApplicationExitInfo,
        threadConsumer: (BugsnagThread) -> Unit
    ) {
        try {
            val trace: Tombstone = exitInfo.traceInputStream?.use {
                Tombstone.newBuilder().mergeFrom(it).build()
            } ?: return
            extractTombstoneThreads(trace.threadsMap.values, threadConsumer)
        } catch (ex: Throwable) {
            logger.w("Tombstone input stream threw an Exception", ex)
        }
    }

    private fun extractTombstoneThreads(
        values: Collection<TombstoneProtos.Thread>,
        threadConsumer: (BugsnagThread) -> Unit
    ) {
        values.forEach { thread ->
            val stacktrace = thread.currentBacktraceList.map { tombstoneTraceFrame ->
                val stackFrame = Stackframe(
                    tombstoneTraceFrame.functionName,
                    tombstoneTraceFrame.fileName,
                    tombstoneTraceFrame.relPc,
                    null
                )
                stackFrame.symbolAddress = tombstoneTraceFrame.functionOffset
                stackFrame.loadAddress = tombstoneTraceFrame.fileMapOffset
                stackFrame.codeIdentifier = tombstoneTraceFrame.buildId
                return@map stackFrame
            }

            val bugsnagThread = BugsnagThread(
                thread.id.toString(),
                thread.name,
                ErrorType.C,
                false,
                State.UNKNOWN,
                logger
            )
            bugsnagThread.stacktrace = stacktrace
            threadConsumer(bugsnagThread)
        }
    }
}
