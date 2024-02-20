package com.bugsnag.android

import androidx.annotation.VisibleForTesting
import com.bugsnag.android.ndk.getCString
import com.bugsnag.android.ndk.getNativeBool
import com.bugsnag.android.ndk.getNativeInt
import com.bugsnag.android.ndk.getNativeLong
import com.bugsnag.android.ndk.getNativeSize
import com.bugsnag.android.ndk.getNativeTime
import com.bugsnag.android.ndk.realign
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import java.util.Date

private const val BUGSNAG_EVENT_VERSION = 13
private const val BUGSNAG_FRAMES_MAX = 192

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
        decodeUser(eventBytes, event)
        decodeError(eventBytes, event)

        return event
    }

    private fun decodeNotifier(eventBytes: ByteBuffer, event: Event) {
        eventBytes.realign()
        val name = eventBytes.getCString(64)
        val version = eventBytes.getCString(16)
        val url = eventBytes.getCString(64)

        event.session?.notifier?.name = name
        event.session?.notifier?.version = version
        event.session?.notifier?.url = url
    }

    private fun decodeAppInfoToAppWithState(eventBytes: ByteBuffer, event: Event) {
        eventBytes.realign()
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
    }

    private fun decodeDeviceInfo(eventBytes: ByteBuffer, event: Event) {
        eventBytes.realign()
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
    }

    private fun decodeUser(eventBytes: ByteBuffer, event: Event) {
        eventBytes.realign()
        val name = eventBytes.getCString(64)
        val email = eventBytes.getCString(64)
        val id = eventBytes.getCString(64)
        event.setUser(id, email, name)
    }

    private fun decodeError(eventBytes: ByteBuffer, event: Event) {
        eventBytes.realign()
        val errorClass = eventBytes.getCString(64)
        val errorMessage = eventBytes.getCString(256)
        val type = eventBytes.getCString(32)
        val frameCount = eventBytes.getNativeSize()
        val stacktrace = Array(BUGSNAG_FRAMES_MAX) { decodeFrame(eventBytes) }
            .take(frameCount.toInt())

        val error = event.errors.single()
        error.errorClass = errorClass
        error.errorMessage = errorMessage
        error.type = try {
            ErrorType.valueOf(type)
        } catch (e: IllegalArgumentException) {
            ErrorType.UNKNOWN
        }
        error.stacktrace.clear()
        error.stacktrace.addAll(stacktrace)
    }

    private fun decodeFrame(eventBytes: ByteBuffer): Stackframe {
        eventBytes.realign()
        val frameAddress = eventBytes.getNativeLong()
        val symbolsAddress = eventBytes.getNativeLong()
        val loadAddress = eventBytes.getNativeLong()
        val lineNumber = eventBytes.getNativeLong()
        val fileName = eventBytes.getCString(256)
        val method = eventBytes.getCString(256)
        val codeIdentifier = eventBytes.getCString(256)
        val result = Stackframe(
            method = null,
            file = null,
            lineNumber = lineNumber,
            inProject = null,
            code = null,
            columnNumber = null
        )
        if (fileName.isNotEmpty()) {
            result.file = fileName
        }

        if (method.isEmpty()) {
            result.method = getDecodedFrameAddress(frameAddress)
        } else {
            result.method = method
        }

        result.frameAddress = frameAddress
        result.symbolAddress = symbolsAddress
        result.loadAddress = loadAddress
        result.codeIdentifier = codeIdentifier

        return result
    }

    private fun getDecodedFrameAddress(frameAddress: Long) = if (frameAddress >= 0) {
        "0x%x".format(frameAddress)
    } else {
        "0x%x%02x".format(frameAddress.ushr(8), frameAddress.and(0xff))
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
