package com.bugsnag.android

import com.bugsnag.android.internal.DateUtils
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
        apiKey: String = map.readEntry("apiKey")
    ): EventInternal {
        logger.d("Read previous journal, contents=$map")
        val event = EventInternal(apiKey)

        // populate user
        val userMap: Map<String, String> = map.readEntry("user")
        event.userImpl = User(userMap)

        // populate metadata
        val metadataMap: Map<String, Map<String, Any?>> = map.readEntry("metaData")
        metadataMap.forEach { (key, value) ->
            event.addMetadata(key, value)
        }

        // populate breadcrumbs
        val breadcrumbList: List<MutableMap<String, Any?>> = map.readEntry("breadcrumbs")
        val crumbs = breadcrumbList
            .map(this::sanitizeBreadcrumbMap)
            .map { Breadcrumb(BreadcrumbInternal(it), logger) }
        event.breadcrumbs.addAll(crumbs)

        // populate context
        event.context = map["context"] as? String

        // populate app
        val appMap: MutableMap<String, Any?> = map.readEntry("app")
        event.app = AppWithState(appMap)
        // populate device
        val deviceMap: MutableMap<String, Any?> = map.readEntry("device")
        event.device = convertDeviceWithState(deviceMap)

        // populate session
        val sessionMap = map["session"] as? Map<String, Any?>
        sessionMap?.let {
            event.session = Session(it, logger)
        }

        val threads = map["threads"] as? List<Map<String, Any?>>
        threads?.mapTo(event.threads) { Thread(convertThreadInternal(it), logger) }

        // populate projectPackages
        val projectPackages = map["projectPackages"] as? List<String>
        projectPackages?.let {
            event.projectPackages = projectPackages
        }

        // populate exceptions
        val exceptions: List<MutableMap<String, Any?>> = map.readEntry("exceptions")
        exceptions.mapTo(event.errors) { Error(convertErrorInternal(it), this.logger) }
        return event
    }

    private fun sanitizeBreadcrumbMap(src: Map<String, Any?>): MutableMap<String, Any?> {
        val map = src.toMutableMap()
        map["message"] = map["name"]
        map.remove("name")

        val type = map["type"] as String
        map["type"] = BreadcrumbType.valueOf(type.toUpperCase(Locale.US))

        map["timestamp"] = (map["timestamp"] as String).toDate()

        map["metadata"] = map["metaData"]
        map.remove("metaData")
        return map
    }

    @Suppress("UNCHECKED_CAST")
    private fun convertDeviceWithState(src: Map<String, Any?>): DeviceWithState {
        val map = src.toMutableMap()
        map["cpuAbi"] = (map["cpuAbi"] as? List<String>)?.toTypedArray()
        map["time"] = (map["time"] as? String)?.toDate()
        return DeviceWithState(map)
    }

    @Suppress("UNCHECKED_CAST")
    private fun convertThreadInternal(src: Map<String, Any?>): ThreadInternal {
        val stacktrace = Stacktrace(
            (src["stacktrace"] as? List<Map<String, Any?>>)?.map { Stackframe(it) }
                ?: emptyList()
        )

        return ThreadInternal(
            src.readEntry("id"),
            src.readEntry("name"),
            src.readEntry<String>("type")
                .let { type -> ThreadType.values().find { it.desc == type } }
                ?: ThreadType.ANDROID,
            src["errorReportingThread"] == true,
            src.readEntry("state"),
            stacktrace
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun convertErrorInternal(src: Map<String, Any?>): ErrorInternal {
        val map = src.toMutableMap()
        map["stacktrace"] =
            (src["stacktrace"] as List<Map<String, Any>>).map(this::convertStacktraceInternal)
        return ErrorInternal(map)
    }

    private fun convertStacktraceInternal(frame: Map<String, Any>): MutableMap<String, Any?> {
        val copy: MutableMap<String, Any?> = frame.toMutableMap()
        val lineNumber = frame["lineNumber"] as? Number
        copy["lineNumber"] = lineNumber?.toLong()

        (frame["frameAddress"] as? String)?.let {
            copy["frameAddress"] = java.lang.Long.decode(it)
        }

        (frame["symbolAddress"] as? String)?.let {
            copy["symbolAddress"] = java.lang.Long.decode(it)
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
