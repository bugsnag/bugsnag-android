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
        map: Map<in String, Any?>,
        apiKey: String = map.readEntry(JournalKeys.pathApiKey)
    ): EventInternal {
        logger.d("Read previous journal, contents=$map")
        val event = EventInternal(apiKey)

        // populate exceptions. check this early to avoid unnecessary serialization if
        // no stacktrace was gathered.
        val exceptions: List<MutableMap<String, Any?>> = map.readEntry(JournalKeys.pathExceptions)
        exceptions.mapTo(event.errors) { Error(convertErrorInternal(it), this.logger) }

        // populate user
        val userMap: Map<String, String> = map.readEntry(JournalKeys.pathUser)
        event.userImpl = User(userMap)

        // populate metadata
        val metadataMap: Map<String, Map<String, Any?>> = normalized(map.readEntry(JournalKeys.pathMetadata))
        metadataMap.forEach { (key, value) ->
            event.addMetadata(key, value)
        }

        // populate breadcrumbs
        val breadcrumbList: List<MutableMap<String, Any?>> =
            map.readEntry(JournalKeys.pathBreadcrumbs)
        val crumbs = breadcrumbList
            .map(this::sanitizeBreadcrumbMap)
            .map { Breadcrumb(BreadcrumbInternal(it), logger) }
        event.breadcrumbs.addAll(crumbs)

        // populate context
        event.context = map[JournalKeys.pathContext] as? String

        // populate groupingHash
        event.groupingHash = map[JournalKeys.pathGroupingHash] as? String

        // populate app
        val appMap: MutableMap<String, Any?> = map.readEntry(JournalKeys.pathApp)
        appMap[JournalKeys.keyDuration] = getDuration(map)
        appMap[JournalKeys.keyDurationInFG] = getDurationInFG(map)
        event.app = convertAppWithState(appMap)

        // populate device
        val deviceMap: MutableMap<String, Any?> = map.readEntry(JournalKeys.pathDevice)
        event.device = convertDeviceWithState(deviceMap)

        // populate session
        val sessionMap = map[JournalKeys.pathSession] as? Map<String, Any?>
        sessionMap?.let {
            event.session = Session(it, logger)
        }

        // populate threads
        val threads = map[JournalKeys.pathThreads] as? List<Map<String, Any?>>
        threads?.mapTo(event.threads) { Thread(convertThreadInternal(it), logger) }

        // populate projectPackages
        val projectPackages = map[JournalKeys.pathProjectPackages] as? List<String>
        projectPackages?.let {
            event.projectPackages = projectPackages
        }

        // populate severity
        val severityStr: String = map.readEntry(JournalKeys.pathSeverity)
        val severity = Severity.fromDescriptor(severityStr)
        val unhandled: Boolean = map.readEntry(JournalKeys.keyUnhandled)
        val reason = deserializeSeverityReason(map, unhandled, severity)
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

        map[JournalKeys.keyTimestamp] = (map[JournalKeys.keyTimestamp] as String).toDate()

        map["metadata"] = map[JournalKeys.keyMetadata]
        map.remove(JournalKeys.keyMetadata)
        return map
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

    private fun getLaunchedTime(journal: Map<in String, Any?>): Date? {
        val runtime = journal.readEntry<Map<String, Any?>>(JournalKeys.pathRuntime)
        return convertFieldToDate(runtime[JournalKeys.keyTimeLaunched])
    }

    private fun getEventTime(journal: Map<in String, Any?>): Date? {
        val runtime = journal.readEntry<Map<String, Any?>>(JournalKeys.pathRuntime)
        return convertFieldToDate(runtime[JournalKeys.keyTimeNow])
    }

    private fun getEnteredFGTime(journal: Map<in String, Any?>): Date? {
        val runtime = journal.readEntry<Map<String, Any?>>(JournalKeys.pathRuntime)
        return convertFieldToDate(runtime[JournalKeys.keyTimeEnteredForeground])
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
        return DeviceWithState(map, map.readEntry(JournalKeys.keyRuntimeVersions))
    }

    @Suppress("UNCHECKED_CAST")
    private fun convertThreadInternal(src: Map<String, Any?>): ThreadInternal {
        val stacktrace = Stacktrace(
            (src[JournalKeys.keyStackTrace] as? List<Map<String, Any?>>)?.map { Stackframe(it) }
                ?: emptyList()
        )

        return ThreadInternal(
            src.readEntry<Number>(JournalKeys.keyId).toLong(),
            src.readEntry(JournalKeys.keyName),
            src.readEntry<String>(JournalKeys.keyType)
                .let { type -> ThreadType.values().find { it.desc == type } }
                ?: ThreadType.ANDROID,
            src[JournalKeys.keyErrorReportingThread] == true,
            src.readEntry(JournalKeys.keyState),
            stacktrace
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun convertErrorInternal(src: Map<String, Any?>): ErrorInternal {
        val map = src.toMutableMap()
        val list: List<Map<String, Any>> = src.readEntry(JournalKeys.keyStackTrace)
        if (list.isNotEmpty()) {
            map[JournalKeys.keyStackTrace] = list.map(this::convertStacktraceInternal)
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
        val severityReason: Map<String, Any> = map.readEntry(JournalKeys.pathSeverityReason)
        val unhandledOverridden: Boolean =
            severityReason.readEntry(JournalKeys.keyUnhandledOverridden)
        val type: String = severityReason.readEntry(JournalKeys.keyType)
        val originalUnhandled = when {
            unhandledOverridden -> !unhandled
            else -> unhandled
        }

        val attrMap: Map<String, String>? = severityReason.readEntry(JournalKeys.keyAttributes)
        val entry = attrMap?.entries?.singleOrNull()
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
     * Convenience method for getting an entry from a Map in the expected type, which
     * throws useful error messages if the expected type is not there.
     */
    private inline fun <reified T> Map<*, *>.readEntry(key: String): T {
        when (val value = get(key)) {
            is T -> return value
            null -> throw IllegalStateException("Journal does not contain entry for '$key'")
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
