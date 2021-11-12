package com.bugsnag.android

import com.bugsnag.android.internal.DateUtils
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

internal class BugsnagEventMapper(
    private val logger: Logger
) {
    internal fun deserializeSeverityReason(
        map: Map<in String, Any?>,
        unhandled: Boolean,
        severity: Severity?
    ): SeverityReason {
        val severityReason: Map<String, Any> = map.readEntry("severityReason")
        val unhandledOverridden: Boolean =
            severityReason.readEntry("unhandledOverridden")
        val type: String = severityReason.readEntry("type")
        val originalUnhandled = when {
            unhandledOverridden -> !unhandled
            else -> unhandled
        }

        val attrMap: Map<String, String>? = severityReason.readEntry("attributes")
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
            null -> throw IllegalStateException("cannot find json property '$key'")
            else -> throw IllegalArgumentException(
                "json property '$key' not " +
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