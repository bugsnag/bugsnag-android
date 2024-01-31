package com.bugsnag.android

import androidx.annotation.VisibleForTesting
import com.bugsnag.android.ndk.getCString
import com.bugsnag.android.ndk.getNativeInt
import com.bugsnag.android.ndk.getNativeLong
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel

private const val BUGSNAG_EVENT_VERSION = 13

@Suppress("MagicNumber") // this class is filled with numbers defined in event.h
internal object NativeEventDecoder {

    fun decode(
        client: Client,
        event: File,
        @Suppress("UNUSED_PARAMETER")
        staticData: File = File(event.parentFile, event.name + ".static_data.json")
    ): Event {

        val newEvent = NativeInterface.createEvent(
            null,
            client,
            SeverityReason.newInstance(SeverityReason.REASON_HANDLED_EXCEPTION)
        )

        return event.inputStream().channel.use {
            // map the entire file in READ_ONLY mode
            val eventBytes = it.map(FileChannel.MapMode.READ_ONLY, 0L, event.length())
            decodeEventFromBytes(eventBytes, newEvent)
        }
    }

    @VisibleForTesting
    internal fun decodeEventFromBytes(
        eventBytes: ByteBuffer,
        event: Event
    ): Event {
        eventBytes.order(ByteOrder.nativeOrder())

        val header = decodeHeader(eventBytes)
        require(header.version == BUGSNAG_EVENT_VERSION) { "Unsupported event version: ${header.version}" }

        if (header.bigEndian == 0) {
            eventBytes.order(ByteOrder.BIG_ENDIAN)
        }

        decodeNotifier(eventBytes, event)
        decodeAppInfoToAppWithState(eventBytes, event)

        return event
//        @Suppress("StopShip") // This is targeting an integration branch
//        TODO("To be completed")
    }

    private fun decodeNotifier(eventBytes: ByteBuffer, event: Event) {
        event.session?.notifier?.name = eventBytes.getCString(64)
        event.session?.notifier?.version = eventBytes.getCString(16)
        event.session?.notifier?.url = eventBytes.getCString(64)
    }

    private fun decodeAppInfoToAppWithState(eventBytes: ByteBuffer, event: Event) {
        val id = eventBytes.getCString(64)
        val releaseStage = eventBytes.getCString(64)
        val type = eventBytes.getCString(32)
        val version = eventBytes.getCString(32)
        val versionCode = eventBytes.getNativeLong()
        val buildUuid = eventBytes.getCString(64)
        val duration = eventBytes.getNativeLong()
        val durationInForeground = eventBytes.getNativeLong()
        val inForeground = eventBytes.getNativeInt() != 0
        val isLaunching = eventBytes.getNativeInt() != 0
        val binaryArch = eventBytes.getCString(32)

        event.app = AppWithState(
            binaryArch = binaryArch,
            id = id,
            releaseStage = releaseStage,
            version = version,
            codeBundleId = null,
            buildUuid = buildUuid,
            type = type,
            versionCode = versionCode,
            duration = duration,
            durationInForeground = durationInForeground,
            inForeground = inForeground,
            isLaunching = isLaunching
        )
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
