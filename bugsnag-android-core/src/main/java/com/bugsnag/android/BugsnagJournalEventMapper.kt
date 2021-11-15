package com.bugsnag.android

import com.bugsnag.android.internal.DateUtils
import com.bugsnag.android.internal.journal.JournalKeys
import com.bugsnag.android.internal.journal.normalized
import java.io.File
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Converts a Bugsnag journal entry into an Event.
 */
internal class BugsnagJournalEventMapper(
    private val logger: Logger
) {

    fun convertToEvent(baseDocumentPath: File): EventInternal? {
        return try {
            when (val map = BugsnagJournal.loadPreviousDocument(baseDocumentPath)) {
                null -> null
                else -> convertToEvent(map)
            }
        } catch (exc: Throwable) {
            logger.e("Failed to load journal, skipping event", exc)
            null
        }
    }

    fun convertToEvent(map: Map<in String, Any?>): EventInternal? {
        return try {
            convertToEventImpl(map)
        } catch (exc: Throwable) {
            logger.e("Failed to deserialize journal, skipping event", exc)
            null
        }
    }

    @Suppress("UNCHECKED_CAST")
    internal fun convertToEventImpl(
        journalData: Map<in String, Any?>,
        apiKey: String = journalData.readEntryOrDefault(JournalKeys.pathApiKey, "")
    ): EventInternal {
        val event = EventInternal(apiKey)

        // populate exceptions. check this early to avoid unnecessary serialization if
        // no stacktrace was gathered.
        (journalData[JournalKeys.pathExceptions] as? List<MutableMap<String, Any?>>)?.let {
            it.mapTo(event.errors) { Error(convertErrorInternal(it), this.logger) }
        }

        // populate user
        (journalData[JournalKeys.pathUser] as? Map<String, String>)?.let {
            event.userImpl = User(it)
        }

        // populate metadata
        (journalData[JournalKeys.pathMetadata] as? Map<String, Map<String, Any?>>)?.let {
            val norm = normalized<Map<String, Map<String, Any?>>>(it)
            norm.forEach { (key, value) ->
                event.addMetadata(key, value)
            }
        }

        // populate breadcrumbs
        (journalData[JournalKeys.pathBreadcrumbs] as? List<MutableMap<String, Any?>>)?.let {
            val crumbs = it
                .map(this::sanitizeBreadcrumbMap)
                .map { Breadcrumb(BreadcrumbInternal(it), logger) }
            event.breadcrumbs.addAll(crumbs)
        }

        // populate context
        event.context = journalData[JournalKeys.pathContext] as? String

        // populate groupingHash
        event.groupingHash = journalData[JournalKeys.pathGroupingHash] as? String

        // populate app
        (journalData[JournalKeys.pathApp] as? MutableMap<String, Any?>)?.let {
            // if loading from journal, calculate duration from 'runtime' entry
            if (journalData[JournalKeys.pathRuntime] != null) {
                it[JournalKeys.keyDuration] = getDuration(journalData)
                it[JournalKeys.keyDurationInFG] = getDurationInFG(journalData)
            }
            event.app = convertAppWithState(it)
        }

        // populate device
        (journalData[JournalKeys.pathDevice] as? MutableMap<String, Any?>)?.let {
            event.device = convertDeviceWithState(it)
        }

        // populate session
        (journalData[JournalKeys.pathSession] as? Map<String, Any?>)?.let {
            event.session = Session(it, logger)
        }

        // populate threads
        (journalData[JournalKeys.pathThreads] as? List<Map<String, Any?>>)?.mapTo(event.threads) {
            Thread(convertThreadInternal(it), logger)
        }

        // populate projectPackages
        (journalData[JournalKeys.pathProjectPackages] as? List<String>)?.let {
            event.projectPackages = it
        }

        // populate severity
        val severityStr: String = journalData.readEntryOrDefault(JournalKeys.pathSeverity, "")
        val severity = Severity.fromDescriptor(severityStr)
        val unhandled: Boolean = journalData.readEntryOrDefault(JournalKeys.keyUnhandled, true)
        val reason = deserializeSeverityReason(journalData, unhandled, severity)
        event.updateSeverityReasonInternal(reason)
        event.normalizeStackframeErrorTypes()
        return event
    }

    private fun sanitizeBreadcrumbMap(src: Map<String, Any?>): MutableMap<String, Any?> {
        val map = src.toMutableMap()
        map["message"] = map[JournalKeys.keyName]
        map.remove(JournalKeys.keyName)

        val type = map[JournalKeys.keyType] as String
        map[JournalKeys.keyType] = BreadcrumbType.valueOf(type.toUpperCase(Locale.US))
        map[JournalKeys.keyTimestamp] = sanitizeBreadcrumbTimestamp(map)

        map["metadata"] = map[JournalKeys.keyMetadata]
        map.remove(JournalKeys.keyMetadata)
        return map
    }

    private fun sanitizeBreadcrumbTimestamp(map: MutableMap<String, Any?>): Date {
        return when (val time = map[JournalKeys.keyTimestamp]) {
            is Long -> Date(time)
            is String -> time.toDate()
            else -> throw IllegalStateException("Expected long/string for breadcrumb timestamp")
        }
    }

    private fun convertHexToLongSafe(hex: String): Long {
        val base16 = 16
        val hexMaxChars = 16
        val shiftToHighByte = 56

        return try {
            hex.toLong(base16)
        } catch (e: NumberFormatException) {
            if (hex.length != hexMaxChars) {
                // NumberFormatException wasn't caused by high bit being set
                throw e
            }
            // Value is > max signed long, so overflow it to negative.
            val highByte = hex.substring(0, 2).toLong(base16) shl shiftToHighByte
            if (highByte >= 0) {
                // NumberFormatException wasn't caused by high bit being set
                throw e
            }
            highByte + hex.substring(2).toLong(base16)
        }
    }

    private fun convertFieldToLong(value: Any?): Long {
        val offset0x = 2
        return when (value) {
            is String -> {
                if (value.startsWith("0x")) {
                    return convertHexToLongSafe(value.substring(offset0x))
                } else {
                    0
                }
            }
            is Long -> value
            is Int -> value.toLong()
            is Date -> value.time
            else -> 0
        }
    }

    private fun convertFieldToDate(value: Any?): Date? {
        return when (value) {
            is String -> {
                if (value.startsWith("0x")) {
                    Date(convertFieldToLong(value))
                } else {
                    value.toDate()
                }
            }
            else -> null
        }
    }

    private fun getRuntimeValue(journal: Map<in String, Any?>, runtimeKey: String): Any? {
        @Suppress("UNCHECKED_CAST")
        return (journal[JournalKeys.pathRuntime] as? Map<String, Any?>)?.let {
            it[runtimeKey]
        }
    }

    private fun getLaunchedTime(journal: Map<in String, Any?>): Date? {
        return convertFieldToDate(getRuntimeValue(journal, JournalKeys.keyTimeLaunched))
    }

    private fun getEventTime(journal: Map<in String, Any?>): Date? {
        return convertFieldToDate(getRuntimeValue(journal, JournalKeys.keyTimeNow))
    }

    private fun getEnteredFGTime(journal: Map<in String, Any?>): Date? {
        return convertFieldToDate(getRuntimeValue(journal, JournalKeys.keyTimeEnteredForeground))
    }

    private fun getDuration(journal: Map<in String, Any?>): Long {
        val launchTime = getLaunchedTime(journal)
        val eventTime = getEventTime(journal)
        if (eventTime == null || launchTime == null) {
            return 0
        }
        return eventTime.time - launchTime.time
    }

    private fun getDurationInFG(journal: Map<in String, Any?>): Long {
        val enteredFGTime = getEnteredFGTime(journal)
        val eventTime = getEventTime(journal)
        if (eventTime == null || enteredFGTime == null) {
            return 0
        }
        return eventTime.time - enteredFGTime.time
    }

    @Suppress("UNCHECKED_CAST")
    private fun convertAppWithState(src: Map<String, Any?>): AppWithState {
        val map = src.toMutableMap()
        map["buildUuid"] = map[JournalKeys.keyBuildUUID] as? String
        return AppWithState(map)
    }

    @Suppress("UNCHECKED_CAST")
    private fun convertDeviceWithState(src: Map<String, Any?>): DeviceWithState {
        val map = src.toMutableMap()
        map[JournalKeys.keyCpuAbi] = (map[JournalKeys.keyCpuAbi] as? List<String>)?.toTypedArray()
        map[JournalKeys.keyTime] = convertFieldToDate(map[JournalKeys.keyTime])
        return DeviceWithState(map, map.readEntryOrDefault(JournalKeys.keyRuntimeVersions, mutableMapOf()))
    }

    @Suppress("UNCHECKED_CAST")
    private fun convertThreadInternal(src: Map<String, Any?>): ThreadInternal {
        val stacktrace = Stacktrace(
            (src[JournalKeys.keyStackTrace] as? List<Map<String, Any?>>)?.map { Stackframe(it) }
                ?: emptyList()
        )

        return ThreadInternal(
            src.readEntryOrDefault<Number>(JournalKeys.keyId, 0).toLong(),
            src.readEntryOrDefault(JournalKeys.keyName, ""),
            src.readEntryOrDefault<String>(JournalKeys.keyType, "")
                .let { type -> ThreadType.values().find { it.desc == type } }
                ?: ThreadType.ANDROID,
            src[JournalKeys.keyErrorReportingThread] == true,
            src.readEntryOrDefault(JournalKeys.keyState, ""),
            stacktrace
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun convertErrorInternal(src: Map<String, Any?>): ErrorInternal {
        val map = src.toMutableMap()
        (src[JournalKeys.keyStackTrace] as? List<Map<String, Any>>)?.let {
            map[JournalKeys.keyStackTrace] = it.map(this::convertStacktraceInternal)
        }
        return ErrorInternal(map)
    }

    private fun convertStacktraceInternal(frame: Map<String, Any>): MutableMap<String, Any?> {
        val copy: MutableMap<String, Any?> = frame.toMutableMap()
        val lineNumber = frame[JournalKeys.keyLineNumber] as? Number
        copy[JournalKeys.keyLineNumber] = lineNumber?.toLong()

        (frame[JournalKeys.keyFrameAddress] as? String)?.let {
            copy[JournalKeys.keyFrameAddress] = java.lang.Long.decode(it)
        }

        (frame[JournalKeys.keySymbolAddress] as? String)?.let {
            copy[JournalKeys.keySymbolAddress] = java.lang.Long.decode(it)
        }

        (frame[JournalKeys.keyLoadAddress] as? String)?.let {
            copy[JournalKeys.keyLoadAddress] = java.lang.Long.decode(it)
        }

        (frame[JournalKeys.keyIsPC] as? Boolean)?.let {
            copy[JournalKeys.keyIsPC] = it
        }
        return copy
    }

    private fun deserializeSeverityReason(
        map: Map<in String, Any?>,
        unhandled: Boolean,
        severity: Severity
    ): SeverityReason {
        val severityReason: Map<String, Any> = map.readEntryOrDefault(JournalKeys.pathSeverityReason, mapOf())
        val unhandledOverridden: Boolean =
            severityReason.readEntryOrDefault(JournalKeys.keyUnhandledOverridden, false)
        val type: String = severityReason.readEntryOrDefault(JournalKeys.keyType, "")
        val originalUnhandled = when {
            unhandledOverridden -> !unhandled
            else -> unhandled
        }

        @Suppress("UNCHECKED_CAST")
        val entry = (severityReason[JournalKeys.keyAttributes] as? Map<String, String>)?.entries?.singleOrNull()
        return SeverityReason(
            type,
            severity,
            unhandled,
            originalUnhandled,
            entry?.value,
            entry?.key
        )
    }

    /**
     * Convenience method for getting an entry from a Map in the expected type.
     */
    private inline fun <reified T> Map<*, *>.readEntryOrDefault(key: String, defaultValue: T): T {
        when (val value = get(key)) {
            is T -> return value
            null -> return defaultValue
            else -> throw IllegalArgumentException(
                "Journal entry for '$key' not " +
                    "of expected type, found ${value.javaClass.name}"
            )
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

    private fun String.toDate(): Date {
        return try {
            DateUtils.fromIso8601(this)
        } catch (pe: IllegalArgumentException) {
            ndkDateFormatHolder.get()!!.parse(this)
                ?: throw IllegalArgumentException("cannot parse date $this")
        }
    }
}
