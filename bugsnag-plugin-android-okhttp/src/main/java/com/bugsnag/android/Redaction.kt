package com.bugsnag.android

private const val REDACTED_PLACEHOLDER = "[REDACTED]"

/**
 * Replaces any values in the map that match [Configuration.getRedactedKeys] with a
 * placeholder.
 */
internal fun Client.redactMap(data: Map<String, Any?>): Map<String, Any?> {
    return data.mapValues { entry ->
        val redactedKeys = config.redactedKeys

        when {
            redactedKeys.contains(entry.key) -> REDACTED_PLACEHOLDER
            else -> entry.value
        }
    }
}
