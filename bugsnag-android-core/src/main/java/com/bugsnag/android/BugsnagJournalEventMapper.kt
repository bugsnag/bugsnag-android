package com.bugsnag.android

import com.bugsnag.android.internal.DateUtils
import com.bugsnag.android.internal.journal.JournalKeys
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

        // populate user
        val userMap: Map<String, String> = map.readEntry(JournalKeys.pathUser)
        event.userImpl = User(userMap)

        // populate metadata
        val metadataMap: Map<String, Map<String, Any?>> = map.readEntry(JournalKeys.pathMetadata)
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
        event.app = convertAppWithState(appMap)

        // populate device
        val deviceMap: MutableMap<String, Any?> = map.readEntry(JournalKeys.pathDevice)
        event.device = convertDeviceWithState(deviceMap)

        // populate session
        val sessionMap = map[JournalKeys.pathSession] as? Map<String, Any?>
        sessionMap?.let {
            event.session = Session(it, logger)
        }

        val threads = map[JournalKeys.pathThreads] as? List<Map<String, Any?>>
        threads?.mapTo(event.threads) { Thread(convertThreadInternal(it), logger) }

        // populate projectPackages
        val projectPackages = map[JournalKeys.pathProjectPackages] as? List<String>
        projectPackages?.let {
            event.projectPackages = projectPackages
        }

        // populate exceptions
        val exceptions: List<MutableMap<String, Any?>> = map.readEntry(JournalKeys.pathExceptions)
        exceptions.mapTo(event.errors) { Error(convertErrorInternal(it), this.logger) }

        // populate severity
        val severityStr: String = map.readEntry(JournalKeys.pathSeverity)
        val severity = Severity.fromDescriptor(severityStr)
        val unhandled: Boolean = map.readEntry(JournalKeys.keyUnhandled)

        val severityReason: Map<String, Any> = map.readEntry(JournalKeys.pathSeverityReason)
        val unhandledOverridden: Boolean = severityReason.readEntry(JournalKeys.keyUnhandledOverridden)
        val type: String = severityReason.readEntry(JournalKeys.keyType)

        val originalUnhandled = when {
            unhandledOverridden -> !unhandled
            else -> unhandled
        }
        val reason = SeverityReason(type, severity, unhandled, originalUnhandled, null)
        event.updateSeverityReasonInternal(reason)
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
        map[JournalKeys.keyTime] = (map[JournalKeys.keyTime] as? String)?.toDate()
        return DeviceWithState(map)
    }

    @Suppress("UNCHECKED_CAST")
    private fun convertThreadInternal(src: Map<String, Any?>): ThreadInternal {
        val stacktrace = Stacktrace(
            (src[JournalKeys.keyStackTrace] as? List<Map<String, Any?>>)?.map { Stackframe(it) }
                ?: emptyList()
        )

        return ThreadInternal(
            src.readEntry(JournalKeys.keyId),
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
        map[JournalKeys.keyStackTrace] =
            (src[JournalKeys.keyStackTrace] as List<Map<String, Any>>).map(this::convertStacktraceInternal)
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
