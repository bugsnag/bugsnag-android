package com.bugsnag.android

import android.app.ApplicationExitInfo
import android.os.Build
import androidx.annotation.RequiresApi

internal data class ExitInfoKey(val pid: Int, val timestamp: Long) : JsonStream.Streamable {
    @RequiresApi(Build.VERSION_CODES.R)
    constructor(exitInfo: ApplicationExitInfo) :
        this(exitInfo.pid, exitInfo.timestamp)

    override fun toStream(stream: JsonStream) {
        stream.beginObject()
            .name("pid").value(pid)
            .name("timestamp").value(timestamp.toString())
            .endObject()
    }
}
