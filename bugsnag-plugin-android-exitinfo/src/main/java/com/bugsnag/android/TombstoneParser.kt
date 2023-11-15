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
        val newLogList = StringBuilder()
        logBuffersList.forEach { logs ->
            logs.logsList.forEach {
                newLogList.append(it.timestamp).append(' ')
                    .append(it.tid).append(' ')
                    .append(it.tag).append(' ')
                    .append(priorityType(it)).append(' ')
                    .append(it.message)
                    .append('\n')
            }
        }
        logcatConsumer(newLogList.toString())
    }

    private fun priorityType(it: TombstoneProtos.LogMessage): String =
        when (it.priority) {
            VERBOSE -> "V"
            DEBUG -> "D"
            INFO -> "I"
            WARN -> "W"
            ERROR -> "E"
            ASSERT -> "A"
            else -> it.priority.toString()
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
                    method = tombstoneTraceFrame.functionName,
                    file = tombstoneTraceFrame.fileName,
                    lineNumber = tombstoneTraceFrame.relPc,
                    inProject = null
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

    /**
     * from android.util.Log
     */
    companion object {
        private const val VERBOSE = 2
        private const val DEBUG = 3
        private const val INFO = 4
        private const val WARN = 5
        private const val ERROR = 6
        private const val ASSERT = 7
    }
}
