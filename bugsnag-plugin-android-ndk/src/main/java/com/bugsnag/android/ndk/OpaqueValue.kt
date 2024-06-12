package com.bugsnag.android.ndk

import com.bugsnag.android.JsonStream
import java.io.StringWriter

/**
 * Marker class for values that are `BSG_METADATA_OPAQUE_VALUE` in the C layer
 */
internal class OpaqueValue(val json: String) {
    internal companion object {
        private const val MAX_NDK_STRING_LENGTH = 64
        private const val US_ASCII_MAX_CODEPOINT = 127
        private const val INITIAL_BUFFER_SIZE = 256

        fun isStringNDKSupported(value: String): Boolean {
            // anything over 63 characters is definitely not supported
            if (value.length >= MAX_NDK_STRING_LENGTH) return false

            // all chars are US-ASCII valid (0-127)?
            if (value.all { ch: Char -> ch.code <= US_ASCII_MAX_CODEPOINT }) {
                // US-ASCII values shorter than 64 characters are supported directly
                return true
            }

            // easiest way to figure it out at this stage is to UTF-8 encode the string and check
            // it's length as a byte-array
            return value.toByteArray().size < MAX_NDK_STRING_LENGTH
        }

        fun encode(value: Any): String {
            val writer = StringWriter(INITIAL_BUFFER_SIZE)
            writer.use { JsonStream(it).value(value, false) }
            return writer.toString()
        }

        /**
         * Ensure that the given `value` is compatible with the Bugsnag C layer by ensuring it
         * is both a compatible type and fits into the available space. This method can return
         * any one of: `Boolean`, `Number`, `String`, `OpaqueValue` or `null`.
         */
        @JvmStatic
        fun makeSafe(value: Any?): Any? = when {
            value is Boolean -> value
            value is Number -> value
            value is String && isStringNDKSupported(value) -> value
            value is String ||
                value is Map<*, *> ||
                value is Collection<*> ||
                value is Array<*> ->
                OpaqueValue(encode(value))
            else -> null
        }
    }
}
