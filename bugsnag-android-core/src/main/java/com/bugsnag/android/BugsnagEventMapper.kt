package com.bugsnag.android

import android.util.JsonReader
import android.util.JsonToken
import com.bugsnag.android.internal.DateUtils
import com.bugsnag.android.internal.InternalMetricsImpl
import com.bugsnag.android.internal.dag.ValueProvider
import java.io.IOException
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

internal class BugsnagEventMapper(private val logger: Logger) {
    internal fun convertToEvent(reader: JsonReader, apiKey: String): Event {
        return Event(convertToEventImpl(reader, apiKey), logger)
    }

    internal fun convertToEventImpl(reader: JsonReader, apiKey: String): EventInternal {
        val event = EventInternal(apiKey, logger)

        // Initialize default values for properties that might not be in JSON
        var severitySet = false
        var unhandledSet = false
        var severityReasonSet = false
        var appSet = false
        var deviceSet = false

        reader.beginObject()
        while (reader.hasNext()) {
            when (reader.nextName()) {
                "exceptions" -> readExceptions(reader, event)
                "user" -> event.userImpl = convertUser(reader)
                "metaData" -> readMetadata(reader, event)
                "featureFlags" -> readFeatureFlags(reader, event)
                "breadcrumbs" -> readBreadcrumbs(reader, event)
                "context" -> event.context = readNullableString(reader)
                "groupingHash" -> event.groupingHash = readNullableString(reader)
                "app" -> {
                    event.app = convertAppWithState(reader)
                    appSet = true
                }

                "device" -> {
                    event.device = convertDeviceWithState(reader)
                    deviceSet = true
                }

                "session" -> event.session = readSession(reader, logger, apiKey)
                "threads" -> readThreads(reader, event)
                "projectPackages" -> event.projectPackages = readStringArray(reader)
                "severity" -> {
                    val severityStr = reader.nextString()
                    event.severity = Severity.fromDescriptor(severityStr) ?: Severity.WARNING
                    severitySet = true
                }

                "unhandled" -> {
                    event.unhandled = reader.nextBoolean()
                    unhandledSet = true
                }

                "severityReason" -> {
                    val reason = deserializeSeverityReason(reader, event.unhandled, event.severity)
                    event.updateSeverityReasonInternal(reason)
                    severityReasonSet = true
                }

                "usage" -> {
                    @Suppress("UNCHECKED_CAST")
                    event.internalMetrics =
                        InternalMetricsImpl(readGenericMap(reader) as Map<String, Any>?)
                }

                "correlation" -> {
                    val correlation = readCorrelation(reader)
                    correlation?.let { event.traceCorrelation = it }
                }

                else -> reader.skipValue()
            }
        }
        reader.endObject()

        // Ensure all required properties are properly initialized
        if (!severitySet) {
            event.severity = Severity.WARNING
        }

        if (!unhandledSet) {
            event.unhandled = false
        }

        if (!severityReasonSet) {
            // Create a default severity reason if not provided
            val defaultReason = SeverityReason(
                SeverityReason.REASON_HANDLED_EXCEPTION,
                event.severity,
                event.unhandled,
                false,
                null,
                null
            )
            event.updateSeverityReasonInternal(defaultReason)
        }

        // Ensure app and device are initialized if not provided in JSON
        if (!appSet) {
            event.app = AppWithState(
                null,
                null,
                null,
                null,
                null,
                null as String?,
                "android",
                null,
                null,
                null,
                null,
                null
            )
        }

        if (!deviceSet) {
            event.device = DeviceWithState(
                DeviceBuildInfo(null, null, null, null, null, null, null, null, null),
                null, null, null, null, mutableMapOf(), null, null, null, null
            )
        }

        event.normalizeStackframeErrorTypes()
        return event
    }

    private fun readExceptions(reader: JsonReader, event: EventInternal) {
        reader.beginArray()
        while (reader.hasNext()) {
            event.errors.add(Error(convertErrorInternal(reader), logger))
        }
        reader.endArray()
    }

    private fun readMetadata(reader: JsonReader, event: EventInternal) {
        reader.beginObject()
        while (reader.hasNext()) {
            val section = reader.nextName()
            val sectionData = readGenericMap(reader)
            if (sectionData != null) {
                event.addMetadata(section, sectionData)
            }
        }
        reader.endObject()
    }

    private fun readFeatureFlags(reader: JsonReader, event: EventInternal) {
        reader.beginArray()
        while (reader.hasNext()) {
            var featureFlag = ""
            var variant: String? = null

            reader.beginObject()
            while (reader.hasNext()) {
                when (reader.nextName()) {
                    "featureFlag" -> featureFlag = reader.nextString()
                    "variant" -> variant = readNullableString(reader)
                    else -> reader.skipValue()
                }
            }
            reader.endObject()

            event.addFeatureFlag(featureFlag, variant)
        }
        reader.endArray()
    }

    private fun readBreadcrumbs(reader: JsonReader, event: EventInternal) {
        reader.beginArray()
        while (reader.hasNext()) {
            event.breadcrumbs.add(Breadcrumb(convertBreadcrumbInternal(reader), logger))
        }
        reader.endArray()
    }

    private fun readThreads(reader: JsonReader, event: EventInternal) {
        reader.beginArray()
        while (reader.hasNext()) {
            event.threads.add(Thread(convertThread(reader), logger))
        }
        reader.endArray()
    }

    private fun readSession(reader: JsonReader, logger: Logger, apiKey: String): Session? {
        val sessionMap = readGenericMap(reader)
        return sessionMap?.let { Session(it, logger, apiKey) }
    }

    internal fun convertError(reader: JsonReader): Error {
        return Error(convertErrorInternal(reader), logger)
    }

    internal fun convertErrorInternal(reader: JsonReader): ErrorInternal {
        var errorClass = ""
        var message: String? = null
        var type = ""
        var stacktrace: Stacktrace? = null

        reader.beginObject()
        while (reader.hasNext()) {
            when (reader.nextName()) {
                "errorClass" -> errorClass = reader.nextString()
                "message" -> message = readNullableString(reader)
                "type" -> type = reader.nextString()
                "stacktrace" -> stacktrace = convertStacktrace(reader)
                else -> reader.skipValue()
            }
        }
        reader.endObject()

        val errorType = ErrorType.fromDescriptor(type)
            ?: throw IllegalArgumentException("unknown ErrorType: '$type'")

        return ErrorInternal(
            errorClass,
            message,
            type = errorType,
            stacktrace = stacktrace ?: Stacktrace(mutableListOf())
        )
    }

    internal fun convertUser(reader: JsonReader): User {
        var id: String? = null
        var email: String? = null
        var name: String? = null

        reader.beginObject()
        while (reader.hasNext()) {
            when (reader.nextName()) {
                "id" -> id = readNullableString(reader)
                "email" -> email = readNullableString(reader)
                "name" -> name = readNullableString(reader)
                else -> reader.skipValue()
            }
        }
        reader.endObject()

        return User(id, email, name)
    }

    internal fun convertBreadcrumbInternal(reader: JsonReader): BreadcrumbInternal {
        var name = ""
        var type = "manual"
        var metaData: MutableMap<String, Any?>? = null
        var timestamp = ""

        reader.beginObject()
        while (reader.hasNext()) {
            when (reader.nextName()) {
                "name" -> name = reader.nextString()
                "type" -> type = reader.nextString()
                "metaData" -> metaData = readGenericMap(reader)
                "timestamp" -> timestamp = reader.nextString()
                else -> reader.skipValue()
            }
        }
        reader.endObject()

        val breadcrumbType = BreadcrumbType.fromDescriptor(type) ?: BreadcrumbType.MANUAL

        return BreadcrumbInternal(
            name,
            breadcrumbType,
            metaData,
            timestamp.toDate()
        )
    }

    internal fun convertAppWithState(reader: JsonReader): AppWithState {
        var binaryArch: String? = null
        var id: String? = null
        var releaseStage: String? = null
        var version: String? = null
        var codeBundleId: String? = null
        var buildUUID: String? = null
        var type: String? = null
        var versionCode: Int? = null
        var duration: Long? = null
        var durationInForeground: Long? = null
        var inForeground: Boolean? = null
        var isLaunching: Boolean? = null

        reader.beginObject()
        while (reader.hasNext()) {
            when (reader.nextName()) {
                "binaryArch" -> binaryArch = readNullableString(reader)
                "id" -> id = readNullableString(reader)
                "releaseStage" -> releaseStage = readNullableString(reader)
                "version" -> version = readNullableString(reader)
                "codeBundleId" -> codeBundleId = readNullableString(reader)
                "buildUUID" -> buildUUID = readNullableString(reader)
                "type" -> type = readNullableString(reader)
                "versionCode" -> versionCode = readNullableNumber(reader)?.toInt()
                "duration" -> duration = readNullableNumber(reader)?.toLong()
                "durationInForeground" -> durationInForeground =
                    readNullableNumber(reader)?.toLong()

                "inForeground" -> inForeground = readNullableBoolean(reader)
                "isLaunching" -> isLaunching = readNullableBoolean(reader)
                else -> reader.skipValue()
            }
        }
        reader.endObject()

        return AppWithState(
            binaryArch,
            id,
            releaseStage,
            version,
            codeBundleId,
            buildUUID?.let(::ValueProvider),
            type,
            versionCode,
            duration,
            durationInForeground,
            inForeground,
            isLaunching
        )
    }

    internal fun convertDeviceWithState(reader: JsonReader): DeviceWithState {
        var manufacturer: String? = null
        var model: String? = null
        var osVersion: String? = null
        var cpuAbi: Array<String>? = null
        var jailbroken: Boolean? = null
        var deviceId: String? = null
        var locale: String? = null
        var totalMemory: Long? = null
        var runtimeVersions: MutableMap<String, Any> = mutableMapOf()
        var freeDisk: Long? = null
        var freeMemory: Long? = null
        var orientation: String? = null
        var time: Date? = null

        reader.beginObject()
        while (reader.hasNext()) {
            @Suppress("UNCHECKED_CAST")
            when (reader.nextName()) {
                "manufacturer" -> manufacturer = readNullableString(reader)
                "model" -> model = readNullableString(reader)
                "osVersion" -> osVersion = readNullableString(reader)
                "cpuAbi" -> cpuAbi = readStringArray(reader).toTypedArray()
                "jailbroken" -> jailbroken = readNullableBoolean(reader)
                "id" -> deviceId = readNullableString(reader)
                "locale" -> locale = readNullableString(reader)
                "totalMemory" -> totalMemory = readNullableNumber(reader)?.toLong()
                "freeDisk" -> freeDisk = readNullableNumber(reader)?.toLong()
                "freeMemory" -> freeMemory = readNullableNumber(reader)?.toLong()
                "orientation" -> orientation = readNullableString(reader)
                "time" -> time = readNullableString(reader)?.toDate()
                "runtimeVersions" -> runtimeVersions =
                    readGenericMap(reader) as MutableMap<String, Any>? ?: mutableMapOf()
                // Skip unknown fields like "osName" which aren't used in DeviceWithState
                else -> reader.skipValue()
            }
        }
        reader.endObject()

        return DeviceWithState(
            DeviceBuildInfo(
                manufacturer,
                model,
                osVersion,
                null,
                null,
                null,
                null,
                null,
                cpuAbi
            ),
            jailbroken,
            deviceId,
            locale,
            totalMemory,
            runtimeVersions,
            freeDisk,
            freeMemory,
            orientation,
            time
        )
    }

    internal fun convertThread(reader: JsonReader): ThreadInternal {
        var id = ""
        var name = ""
        var type = "android"
        var errorReportingThread = false
        var state = ""
        var stacktrace: Stacktrace? = null

        reader.beginObject()
        while (reader.hasNext()) {
            when (reader.nextName()) {
                "id" -> id = reader.nextString()
                "name" -> name = reader.nextString()
                "type" -> type = reader.nextString()
                "errorReportingThread" -> errorReportingThread = reader.nextBoolean()
                "state" -> state = readNullableString(reader) ?: ""
                "stacktrace" -> stacktrace = convertStacktrace(reader)
                else -> reader.skipValue()
            }
        }
        reader.endObject()

        return ThreadInternal(
            id,
            name,
            ErrorType.fromDescriptor(type) ?: ErrorType.ANDROID,
            errorReportingThread,
            state,
            stacktrace ?: Stacktrace(mutableListOf())
        )
    }

    internal fun convertStacktrace(reader: JsonReader): Stacktrace {
        val frames = mutableListOf<Stackframe>()

        reader.beginArray()
        while (reader.hasNext()) {
            frames.add(convertStackframe(reader))
        }
        reader.endArray()

        return Stacktrace(frames)
    }

    private fun convertStackframe(reader: JsonReader): Stackframe {
        val frameMap = readGenericMap(reader) ?: emptyMap()
        return Stackframe(frameMap)
    }

    internal fun deserializeSeverityReason(
        reader: JsonReader,
        unhandled: Boolean,
        severity: Severity?
    ): SeverityReason {
        var type = ""
        var unhandledOverridden = false
        var attributes: Map<String, String>? = null

        reader.beginObject()
        while (reader.hasNext()) {
            when (reader.nextName()) {
                "type" -> type = reader.nextString()
                "unhandledOverridden" -> unhandledOverridden = reader.nextBoolean()
                "attributes" -> attributes = readStringMap(reader)
                else -> reader.skipValue()
            }
        }
        reader.endObject()

        val originalUnhandled = when {
            unhandledOverridden -> !unhandled
            else -> unhandled
        }

        val entry = attributes?.entries?.singleOrNull()
        return SeverityReason(
            type,
            severity,
            unhandled,
            originalUnhandled,
            entry?.value,
            entry?.key
        )
    }

    // Helper methods for JsonReader
    private fun readNullableString(reader: JsonReader): String? {
        return if (reader.peek() == JsonToken.NULL) {
            reader.nextNull()
            null
        } else {
            reader.nextString()
        }
    }

    private fun readNullableBoolean(reader: JsonReader): Boolean? {
        return if (reader.peek() == JsonToken.NULL) {
            reader.nextNull()
            null
        } else {
            reader.nextBoolean()
        }
    }

    private fun readNullableNumber(reader: JsonReader): Number? {
        return if (reader.peek() == JsonToken.NULL) {
            reader.nextNull()
            null
        } else {
            when (reader.peek()) {
                JsonToken.NUMBER -> {
                    val stringValue = reader.nextString()
                    stringValue.toLongOrNull() ?: stringValue.toDoubleOrNull()
                }

                else -> throw IOException("Expected number")
            }
        }
    }

    private fun readStringArray(reader: JsonReader): List<String> {
        val list = mutableListOf<String>()
        reader.beginArray()
        while (reader.hasNext()) {
            list.add(reader.nextString())
        }
        reader.endArray()
        return list
    }

    private fun readStringMap(reader: JsonReader): Map<String, String>? {
        if (reader.peek() == JsonToken.NULL) {
            reader.nextNull()
            return null
        }

        val map = mutableMapOf<String, String>()
        reader.beginObject()
        while (reader.hasNext()) {
            val key = reader.nextName()
            val value = reader.nextString()
            map[key] = value
        }
        reader.endObject()
        return map
    }

    @Suppress("UNCHECKED_CAST")
    private fun readGenericMap(reader: JsonReader): MutableMap<String, Any?>? {
        if (reader.peek() == JsonToken.NULL) {
            reader.nextNull()
            return null
        }

        val map = mutableMapOf<String, Any?>()
        reader.beginObject()
        while (reader.hasNext()) {
            val key = reader.nextName()
            val value = readGenericValue(reader)
            map[key] = value
        }
        reader.endObject()
        return map
    }

    private fun readGenericValue(reader: JsonReader): Any? {
        return when (reader.peek()) {
            JsonToken.NULL -> {
                reader.nextNull()
                null
            }

            JsonToken.BOOLEAN -> reader.nextBoolean()
            JsonToken.NUMBER -> {
                val stringValue = reader.nextString()
                stringValue.toLongOrNull() ?: stringValue.toDoubleOrNull() ?: stringValue
            }

            JsonToken.STRING -> reader.nextString()
            JsonToken.BEGIN_OBJECT -> readGenericMap(reader)
            JsonToken.BEGIN_ARRAY -> {
                val list = mutableListOf<Any?>()
                reader.beginArray()
                while (reader.hasNext()) {
                    list.add(readGenericValue(reader))
                }
                reader.endArray()
                list
            }

            else -> throw IOException("Unexpected token: ${reader.peek()}")
        }
    }

    private fun readCorrelation(reader: JsonReader): TraceCorrelation? {
        var traceId: String? = null
        var spanId: String? = null

        reader.beginObject()
        while (reader.hasNext()) {
            when (reader.nextName()) {
                "traceId" -> traceId = readNullableString(reader)
                "spanId" -> spanId = readNullableString(reader)
                else -> reader.skipValue()
            }
        }
        reader.endObject()

        val parsedTraceId = parseTraceId(traceId)
        val parsedSpanId = spanId?.parseUnsignedLong()

        return if (parsedTraceId != null && parsedSpanId != null) {
            TraceCorrelation(parsedTraceId, parsedSpanId)
        } else {
            null
        }
    }

    private fun String.toDate(): Date {
        if (isNotEmpty() && this[0] == 't') {
            // date is in the format 't{epoch millis}'
            val timestamp = substring(1)
            timestamp.toLongOrNull()?.let {
                return Date(it)
            }
        }

        return try {
            DateUtils.fromIso8601(this)
        } catch (pe: IllegalArgumentException) {
            ndkDateFormatHolder.get()!!.parse(this)
                ?: throw IllegalArgumentException("cannot parse date $this")
        }
    }

    private fun parseTraceId(traceId: String?): UUID? {
        if (traceId?.length != 32) return null
        val mostSigBits = traceId.substring(0, 16).parseUnsignedLong() ?: return null
        val leastSigBits = traceId.substring(16).parseUnsignedLong() ?: return null

        return UUID(mostSigBits, leastSigBits)
    }

    private fun String.parseUnsignedLong(): Long? {
        if (length != 16) return null
        return try {
            (substring(0, 2).toLong(16) shl 56) or
                    substring(2).toLong(16)
        } catch (nfe: NumberFormatException) {
            null
        }
    }

    // SimpleDateFormat isn't thread safe, cache one instance per thread as needed.
    private val ndkDateFormatHolder = object : ThreadLocal<DateFormat>() {
        override fun initialValue(): DateFormat {
            return SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
        }
    }
}
