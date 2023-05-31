package com.bugsnag.android

import com.bugsnag.android.internal.ImmutableConfig
import java.io.File
import java.util.UUID

/**
 * Represents important information about a session filename.
 * Currently the following information is encoded:
 *
 * uuid - to disambiguate stored error reports
 * timestamp - to sort error reports by time of capture
 */
internal data class SessionFilenameInfo(
    var apiKey: String,
    val timestamp: Long,
    val uuid: String
) {

    fun encode(): String {
        return toFilename(apiKey, timestamp, uuid)
    }

    internal companion object {

        const val uuidLength = 36

        /**
         * Generates a filename for the session in the format
         * "[UUID][timestamp]_v2.json"
         */
        fun toFilename(apiKey: String, timestamp: Long, uuid: String): String {
            return "${apiKey}_${uuid}${timestamp}_v2.json"
        }

        @JvmStatic
        fun defaultFilename(apiKey: String, config: ImmutableConfig): String {
            val sanitizedApiKey = apiKey.takeUnless { it.isEmpty() } ?: config.apiKey

            return toFilename(
                sanitizedApiKey,
                System.currentTimeMillis(),
                UUID.randomUUID().toString()
            )
        }

        fun fromFile(file: File, config: ImmutableConfig): SessionFilenameInfo {
            return SessionFilenameInfo(
                findApiKeyInFilename(file, config),
                findTimestampInFilename(file),
                findUuidInFilename(file)
            )
        }

        private fun findUuidInFilename(file: File): String {
            val uuidWithTimestamp = file.name.substringAfter("_")
            val uuidName = if (uuidWithTimestamp.length >= uuidLength) {
                uuidWithTimestamp.take(uuidLength)
            } else {
                null
            }
            return uuidName.takeUnless { it.isNullOrBlank() } ?: ""
        }

        @JvmStatic
        fun findTimestampInFilename(file: File): Long {
            return file.name.substringAfter("_").drop(uuidLength)
                .toLongOrNull() ?: -1
        }

        fun findApiKeyInFilename(file: File, config: ImmutableConfig): String {
            val apiKey = if (file.name.indexOf("_") == 0) {
                null
            } else {
                file.name.substringBefore("_", missingDelimiterValue = "-1")
            }
            return apiKey.takeUnless { it.isNullOrBlank() } ?: config.apiKey
        }
    }
}
