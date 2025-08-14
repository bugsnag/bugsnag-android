package com.bugsnag.android.test

import android.app.ApplicationExitInfo
import org.mockito.Mockito.mock
import java.io.InputStream
import org.mockito.Mockito.`when` as whenever

fun mockAppExitInfo(pid: Int, timestamp: Long, reason: Int): ApplicationExitInfo {
    return mock<ApplicationExitInfo>().also { info ->
        whenever(info.pid).thenReturn(pid)
        whenever(info.timestamp).thenReturn(timestamp)
        whenever(info.reason).thenReturn(reason)
    }
}

fun mockAppExitInfo(
    pid: Int,
    timestamp: Long,
    reason: Int,
    processStateSummery: ByteArray?,
    traceFile: (() -> InputStream)? = null,
): ApplicationExitInfo {
    return mock<ApplicationExitInfo>().also { info ->
        whenever(info.pid).thenReturn(pid)
        whenever(info.timestamp).thenReturn(timestamp)
        whenever(info.reason).thenReturn(reason)
        whenever(info.processStateSummary).thenReturn(processStateSummery)

        if (traceFile != null) {
            whenever(info.traceInputStream).then { traceFile() }
        }
    }
}
