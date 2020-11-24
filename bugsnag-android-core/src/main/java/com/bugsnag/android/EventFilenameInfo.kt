package com.bugsnag.android

import java.io.File
import java.util.Locale
import java.util.UUID

/**
 * Represents important information about an event which is encoded/decoded from a filename.
 * Currently the following information is encoded:
 *
 * apiKey - as a user can decide to override the value on an Event
 * uuid - to disambiguate stored error reports
 * timestamp - to sort error reports by time of capture
 * suffix - used to encode whether the app crashed on launch, or the report is not a JVM error
 * errorTypes - a comma delimited string which contains the stackframe types in the error
 */
internal data class EventFilenameInfo(
    val apiKey: String,
    val uuid: String,
    val timestamp: Long,
    val suffix: String,
    val errorTypes: Set<ErrorType>
) {

    /**
     * Generates a filename for the Event in the format
     * "[timestamp]_[apiKey]_[errorTypes]_[UUID]_[startupcrash|not-jvm].json"
     */
    fun encode(): String {
        return String.format(
            Locale.US,
            "%d_%s_%s_%s_%s.json",
            timestamp,
            apiKey,
            serializeErrorTypeHeader(errorTypes),
            uuid,
            suffix
        )
    }

    fun isLaunchCrashReport(): Boolean = suffix == STARTUP_CRASH

    internal companion object {
        private const val STARTUP_CRASH = "startupcrash"
        private const val NON_JVM_CRASH = "not-jvm"

        @JvmOverloads
        fun fromEvent(
            obj: Any,
            uuid: String = UUID.randomUUID().toString(),
            apiKey: String?,
            timestamp: Long = System.currentTimeMillis(),
            config: ImmutableConfig
        ): EventFilenameInfo {
            val sanitizedApiKey = when {
                obj is Event -> obj.apiKey
                apiKey.isNullOrEmpty() -> config.apiKey
                else -> apiKey
            }

            return EventFilenameInfo(
                sanitizedApiKey,
                uuid,
                timestamp,
                findSuffixForEvent(obj, config),
                findErrorTypesForEvent(obj)
            )
        }

        /**
         * Reads event information from a filename.
         */
        fun fromFile(file: File, config: ImmutableConfig): EventFilenameInfo {
            return EventFilenameInfo(
                findApiKeyInFilename(file, config),
                "", // ignore UUID field when reading from file as unused
                -1, // ignore timestamp when reading from file as unused
                findSuffixInFilename(file),
                findErrorTypesInFilename(file)
            )
        }

        /**
         * Retrieves the api key encoded in the filename, or an empty string if this information
         * is not encoded for the given event
         */
        private fun findApiKeyInFilename(file: File, config: ImmutableConfig): String {
            val name = file.name.replace("_$STARTUP_CRASH.json".toRegex(), "")
            val start = name.indexOf("_") + 1
            val end = name.indexOf("_", start)
            val apiKey = if (start == 0 || end == -1 || end <= start) {
                null
            } else {
                name.substring(start, end)
            }
            return apiKey ?: config.apiKey
        }

        /**
         * Retrieves the error types encoded in the filename, or an empty string if this
         * information is not encoded for the given event
         */
        private fun findErrorTypesInFilename(eventFile: File): Set<ErrorType> {
            val name = eventFile.name
            val end = name.lastIndexOf("_", name.lastIndexOf("_") - 1)
            val start = name.lastIndexOf("_", end - 1) + 1

            if (start < end) {
                val encodedValues: List<String> = name.substring(start, end).split(",")
                return ErrorType.values().filter {
                    encodedValues.contains(it.desc)
                }.toSet()
            }
            return emptySet()
        }

        /**
         * Retrieves the error types encoded in the filename, or an empty string if this
         * information is not encoded for the given event
         */
        private fun findSuffixInFilename(eventFile: File): String {
            val name = eventFile.nameWithoutExtension
            val suffix = name.substring(name.lastIndexOf("_") + 1)
            return when (suffix) {
                STARTUP_CRASH, NON_JVM_CRASH -> suffix
                else -> ""
            }
        }

        /**
         * Retrieves the error types for the given event
         */
        private fun findErrorTypesForEvent(obj: Any): Set<ErrorType> {
            return when (obj) {
                is Event -> obj.impl.getErrorTypesFromStackframes()
                else -> setOf(ErrorType.C)
            }
        }

        /**
         * Calculates the suffix for the given event
         */
        private fun findSuffixForEvent(obj: Any, config: ImmutableConfig): String {
            return when (obj) {
                is Event -> {
                    val duration = obj.app.duration
                    if (duration != null && isStartupCrash(duration.toLong(), config)) {
                        STARTUP_CRASH
                    } else {
                        ""
                    }
                }
                else -> {
                    NON_JVM_CRASH
                }
            }
        }

        private fun isStartupCrash(durationMs: Long, config: ImmutableConfig): Boolean {
            return durationMs < config.launchCrashThresholdMs
        }
    }
}

