package com.bugsnag.android

import android.app.ApplicationExitInfo
import android.os.Build
import androidx.annotation.RequiresApi
import com.android.server.os.TombstoneProtos.Tombstone

internal class TombstoneParser(
    private val logger: Logger
) {
    @RequiresApi(Build.VERSION_CODES.R)
    fun parse(exitInfo: ApplicationExitInfo) {
        try {
            val trace: Tombstone = exitInfo.traceInputStream?.use {
                Tombstone.newBuilder().mergeFrom(it).build()
            } ?: return
            trace.threadsMap.values
        } catch (ex: Throwable) {
            logger.w("Tombstone input stream threw an Exception", ex)
        }
    }
}
