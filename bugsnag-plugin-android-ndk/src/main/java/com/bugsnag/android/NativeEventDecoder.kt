package com.bugsnag.android

import androidx.annotation.VisibleForTesting
import com.bugsnag.android.ndk.getCString
import com.bugsnag.android.ndk.getNativeBool
import com.bugsnag.android.ndk.getNativeInt
import com.bugsnag.android.ndk.getNativeTime
import com.bugsnag.android.ndk.realign
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import java.util.Date

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
            eventBytes.order(ByteOrder.nativeOrder())
            decodeEventFromBytes(eventBytes, newEvent)
        }
    }

    @VisibleForTesting
    internal fun decodeEventFromBytes(
        eventBytes: ByteBuffer,
        event: Event
    ): Event {

        val header = decodeHeader(eventBytes)
        require(header.version == BUGSNAG_EVENT_VERSION) { "Unsupported event version: ${header.version}" }

        decodeNotifier(eventBytes, event)
        decodeAppInfoToAppWithState(eventBytes, event)
        decodeDeviceInfo(eventBytes, event)

        return event
    }

    private fun decodeNotifier(eventBytes: ByteBuffer, event: Event) {
        val name = eventBytes.getCString(64)
        val version = eventBytes.getCString(16)
        val url = eventBytes.getCString(64)

        event.session?.notifier?.name = name
        event.session?.notifier?.version = version
        event.session?.notifier?.url = url
        eventBytes.realign()
    }

    private fun decodeAppInfoToAppWithState(eventBytes: ByteBuffer, event: Event) {
        val id = eventBytes.getCString(64)
        val releaseStage = eventBytes.getCString(64)
        val type = eventBytes.getCString(32)
        val version = eventBytes.getCString(32)
        val activeScreen = eventBytes.getCString(64)
        val versionCode = eventBytes.getLong()
        val buildUuid = eventBytes.getCString(64)
        val duration = eventBytes.getLong()
        val durationInForeground = eventBytes.getLong()
        @Suppress("UNUSED_VARIABLE") val durationMsOffset = eventBytes.getLong()
        @Suppress("UNUSED_VARIABLE") val durationInForegroundMsOffset = eventBytes.getNativeTime()
        val inForeground = eventBytes.getNativeBool()
        val isLaunching = eventBytes.getNativeBool()
        val binaryArch = eventBytes.getCString(32)

        event.app = AppWithState(
            binaryArch = binaryArch,
            id = id,
            releaseStage = releaseStage,
            version = version,
            codeBundleId = null,
            buildUuid = buildUuid.takeUnless { it.isEmpty() },
            type = type,
            versionCode = versionCode,
            duration = duration,
            durationInForeground = durationInForeground,
            inForeground = inForeground,
            isLaunching = isLaunching
        )
        event.addMetadata("app", "activeScreen", activeScreen)
        eventBytes.realign()
    }

    private fun decodeDeviceInfo(eventBytes: ByteBuffer, event: Event) {
        val apiLevel = eventBytes.getNativeInt()
        val cpuAbiCount = eventBytes.getNativeInt()
        val cpuAbis = (0 until 8).map { eventBytes.getCString(32) }
            .take(cpuAbiCount)
            .filter { it.isNotEmpty() }
            .toTypedArray()
        val orientation = eventBytes.getCString(32)
        val time = eventBytes.getNativeTime()
        val id = eventBytes.getCString(64)
        val jailbroken = eventBytes.getNativeBool()
        val locale = eventBytes.getCString(32)
        val manufacturer = eventBytes.getCString(64)
        val model = eventBytes.getCString(64)
        val osBuild = eventBytes.getCString(64)
        val osVersion = eventBytes.getCString(64)
        val osName = eventBytes.getCString(64)
        val totalMemory = eventBytes.getLong()

        event.device.manufacturer = manufacturer
        event.device.id = id
        event.device.osName = osName
        event.device.locale = locale
        event.device.osVersion = osVersion
        event.device.model = model
        event.device.orientation = orientation
        event.device.runtimeVersions?.set("apiLevel", apiLevel)
        event.device.runtimeVersions?.set("osBuild", osBuild)
        event.device.cpuAbi = cpuAbis
        event.device.totalMemory = totalMemory
        event.device.jailbroken = jailbroken
        event.device.time = Date(time * 1000L)
        eventBytes.realign()
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
