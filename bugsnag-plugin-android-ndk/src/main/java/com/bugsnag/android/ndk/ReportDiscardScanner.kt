package com.bugsnag.android.ndk

import android.util.JsonReader
import android.util.JsonToken
import androidx.annotation.VisibleForTesting
import com.bugsnag.android.Logger
import com.bugsnag.android.NativeInterface
import java.io.File

internal class ReportDiscardScanner(
    private val logger: Logger,
    private val enabledReleaseStages: Collection<String> =
        NativeInterface.getEnabledReleaseStages() ?: emptySet(),
) {
    /**
     * Checks whether a given report file should be discarded due to its `releaseStage` or any of
     * the configured `discardClasses`.
     *
     * @return true if the report should be discarded instead of being sent
     */
    fun shouldDiscard(report: File): Boolean {
        if (!report.name.endsWith(".json") ||
            report.name.endsWith(".static_data.json")
        ) {
            return true
        }

        return try {
            report.bufferedReader().use { reader ->
                JsonReader(reader).use { json -> shouldDiscard(json) }
            }
        } catch (ex: Exception) {
            false
        }
    }

    @VisibleForTesting
    internal fun shouldDiscard(json: JsonReader): Boolean {
        json.beginObject()
        var pendingAppCheck = true
        var pendingExceptionsCheck = true

        while (json.hasNext() && (pendingAppCheck || pendingExceptionsCheck)) {
            val nextName = json.nextName()
            val discard: Boolean = when (nextName) {
                "app" -> {
                    pendingAppCheck = false
                    shouldDiscardForApp(json)
                }

                "exceptions" -> {
                    pendingExceptionsCheck = false
                    shouldDiscardForExceptions(json)
                }

                else -> {
                    json.skipValue()
                    false
                }
            }

            if (discard) {
                return true
            }
        }

        return false
    }

    private fun shouldDiscardForApp(json: JsonReader): Boolean {
        if (enabledReleaseStages.isEmpty()) {
            json.skipValue()
            return false
        }

        json.beginObject()
        while (json.peek() != JsonToken.END_OBJECT) {
            val nextName = json.nextName()
            when (nextName) {
                "releaseStage" -> {
                    val releaseStage = json.nextString()
                    if (releaseStage !in enabledReleaseStages) {
                        return true
                    }
                    // do not early exit, make sure the entire "app" object is consumed
                    // before returning
                }

                else -> json.skipValue()
            }
        }
        json.endObject()

        return false
    }

    private fun shouldDiscardForExceptions(json: JsonReader): Boolean {
        json.beginArray()

        while (json.peek() != JsonToken.END_ARRAY) {
            if (shouldDiscardException(json)) {
                logger.d("Discarding native report due to errorClass")
                return true
            }
        }

        json.endArray()

        return false
    }

    private fun shouldDiscardException(json: JsonReader): Boolean {
        json.beginObject()

        while (json.peek() != JsonToken.END_OBJECT) {
            val name = json.nextName()
            when (name) {
                "errorClass" -> {
                    val errorClass = json.nextString()
                    if (NativeInterface.isDiscardErrorClass(errorClass)) {
                        return true
                    }
                    // do not early exit, make sure the entire "exception" object is consumed
                    // before returning
                }

                else -> json.skipValue()
            }
        }

        json.endObject()

        return false
    }
}
