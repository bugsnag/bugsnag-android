package com.bugsnag.android.ndk

import androidx.annotation.VisibleForTesting
import com.bugsnag.android.Event
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

private const val BUGSNAG_EVENT_VERSION = 13

@Suppress("MagicNumber") // this class is filled with numbers defined in event.h
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

        val header = decodeHeader(eventBytes)
        require(header.version == BUGSNAG_EVENT_VERSION) { "Unsupported event version: ${header.version}" }

        if (header.bigEndian == 0) {
            eventBytes.order(ByteOrder.BIG_ENDIAN)
        }

        @Suppress("StopShip") // This is targeting an integration branch
        TODO("To be completed")
    }

    private fun decodeHeader(eventBytes: ByteBuffer): NativeEventHeader {
        return NativeEventHeader(
            eventBytes.getNativeInt(),
            eventBytes.getNativeInt(),
            eventBytes.getCString(64)
        )
    }

    private data class NativeEventHeader(
        val version: Int,
        val bigEndian: Int,
        val osBuild: String
    )
}
