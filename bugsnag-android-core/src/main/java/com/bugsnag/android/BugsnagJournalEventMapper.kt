package com.bugsnag.android

import com.bugsnag.android.internal.DateUtils
import java.io.File
import java.util.Locale

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
    private fun convertToEventImpl(map: Map<in String, Any?>): EventInternal {
        logger.d("Read previous journal, contents=$map")
        val apiKey: String = map.readJournalEntry("apiKey")
        val event = EventInternal(apiKey)

        // populate user
        val userMap: Map<String, String> = map.readJournalEntry("user")
        event.userImpl = User(userMap)

        // populate metadata
        val metadataMap: Map<String, Map<String, Any?>> = map.readJournalEntry("metaData")
        metadataMap.forEach { (key, value) ->
            event.addMetadata(key, value)
        }

        // populate breadcrumbs
        val breadcrumbList: List<MutableMap<String, Any?>> = map.readJournalEntry("breadcrumbs")
        val crumbs = breadcrumbList
            .map(this::sanitizeBreadcrumbMap)
            .map { Breadcrumb(BreadcrumbInternal(it), logger) }
        event.breadcrumbs.addAll(crumbs)
        return event
    }

    private fun sanitizeBreadcrumbMap(src: Map<String, Any?>): MutableMap<String, Any?> {
        val map = src.toMutableMap()
        map["message"] = map["name"]
        map.remove("name")

        val type = map["type"] as String
        map["type"] = BreadcrumbType.valueOf(type.toUpperCase(Locale.US))

        val date = map["timestamp"] as String
        map["timestamp"] = DateUtils.fromIso8601(date)

        map["metadata"] = map["metaData"]
        map.remove("metaData")
        return map
    }

    /**
     * Convenience method for getting an entry from a Map in the expected type, which
     * throws useful error messages if the expected type is not there.
     */
    private inline fun <reified T> Map<*, *>.readJournalEntry(key: String): T {
        check(containsKey(key)) {
            "Journal does not contain entry for '$key'"
        }
        val value = requireNotNull(get(key))
        check(value is T) {
            "Journal entry for '$key' not of expected type, found ${value.javaClass.name}"
        }
        return value
    }
}
