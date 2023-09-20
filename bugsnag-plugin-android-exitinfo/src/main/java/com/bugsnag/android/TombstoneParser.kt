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
        listOpenFds: Boolean,
        includeLogcat: Boolean,
        threadConsumer: (BugsnagThread) -> Unit,
        fileDescriptorConsumer: (Int, String, String) -> Unit,
        logcatConsumer: (String) -> Unit
    ) {
        try {
            val trace: Tombstone = exitInfo.traceInputStream?.use {
                Tombstone.newBuilder().mergeFrom(it).build()
            } ?: return
            extractTombstoneThreads(trace.threadsMap.values, threadConsumer)

            if (listOpenFds) {
                extractTombstoneFd(trace.openFdsList, fileDescriptorConsumer)
            }

            if (includeLogcat) {
                extractTombstoneLogBuffers(trace.logBuffersList, logcatConsumer)
            }
        } catch (ex: Throwable) {
            logger.w("Tombstone input stream threw an Exception", ex)
        }
    }

    private fun extractTombstoneLogBuffers(
        logBuffersList: List<TombstoneProtos.LogBuffer>,
        logcatConsumer: (String) -> Unit
    ) {
        val newLogList = mutableListOf<String>()
        var priorityType: String
        logBuffersList.forEach { logs ->
            logs.logsList.forEach {
                priorityType = when (it.priority) {
                    2 -> "VERBOSE"
                    3 -> "DEBUG"
                    4 -> "INFO"
                    5 -> "WARN"
                    6 -> "ERROR"
                    7 -> "ASSERT"
                    else -> it.priority.toString()
                }

                newLogList.add(
                    0,
                    "\n${it.timestamp} ${it.tid} ${it.tag} $priorityType ${it.message}"
                )
            }
        }
        logcatConsumer(newLogList.toString())
    }

    private fun extractTombstoneFd(
        fdsList: List<TombstoneProtos.FD>,
        fDConsumer: (Int, String, String) -> Unit
    ) {
        fdsList.forEach { fd ->
            fDConsumer(fd.fd, fd.path, fd.owner)
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
