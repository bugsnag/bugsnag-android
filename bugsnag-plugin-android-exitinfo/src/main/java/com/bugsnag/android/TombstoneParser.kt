package com.bugsnag.android

import android.app.ApplicationExitInfo
import android.os.Build
import androidx.annotation.RequiresApi
import com.bugsnag.android.repackaged.server.os.TombstoneProtos
import com.bugsnag.android.repackaged.server.os.TombstoneProtos.Tombstone
import com.bugsnag.android.Thread as BugsnagThread
import com.bugsnag.android.Thread.State

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
            convertToBugsnagThread(trace.threadsMap.values, threadConsumer)
        } catch (ex: Throwable) {
            logger.w("Tombstone input stream threw an Exception", ex)
        }
    }

    private fun convertToBugsnagThread(
        values: MutableCollection<TombstoneProtos.Thread>,
        threadConsumer: (BugsnagThread) -> Unit
    ) {
        values.forEach { thread ->
            val stackFrames = thread.currentBacktraceList.map {
                val stackFrame = Stackframe(it.functionName, it.fileName, it.relPc, null)
                stackFrame.symbolAddress = it.functionOffset
                stackFrame.loadAddress = it.fileMapOffset
                stackFrame.codeIdentifier = it.buildId
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
            bugsnagThread.stacktrace = stackFrames
            threadConsumer(bugsnagThread)
        }
    }
}
