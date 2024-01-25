package com.bugsnag.android.ndk

import androidx.annotation.VisibleForTesting
import com.bugsnag.android.Event
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

internal object NativeEventDecoder {
    fun decode(
        event: File,
        @Suppress("UNUSED_PARAMETER")
        staticData: File = File(event.parentFile, event.name + ".static_data.json")
    ): Event {
        return event.inputStream().channel.use {
            // mmap the entire file in READ_ONLY mode
            val eventBytes = it.map(FileChannel.MapMode.READ_ONLY, 0L, event.length())
            decodeEventFromBytes(eventBytes)
        }
    }

    @VisibleForTesting
    internal fun decodeEventFromBytes(
        eventBytes: ByteBuffer
    ): Event {
        eventBytes.order(ByteOrder.nativeOrder())
        @Suppress("StopShip") // This is targeting an integration branch
        TODO("To be completed")
    }
}
