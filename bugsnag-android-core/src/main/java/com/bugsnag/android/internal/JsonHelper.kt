package com.bugsnag.android.internal

import com.bugsnag.android.JsonStream
import java.io.ByteArrayOutputStream
import java.io.PrintWriter

internal object JsonHelper {

    fun serialize(streamable: JsonStream.Streamable): ByteArray {
        return ByteArrayOutputStream().use { baos ->
            JsonStream(baos.writer()).value(streamable)
            baos.toByteArray()
        }
    }

    fun serialize(value: Any): ByteArray {
        return ByteArrayOutputStream().use { baos ->
            JsonStream(PrintWriter(baos)).use { it.value(value) }
            baos.toByteArray()
        }
    }

    /**
     * Convert a long that technically contains an unsigned long value into its (unsigned) hex string equivalent.
     * Negative values are interpreted as if the sign bit is the high bit of an unsigned integer.
     *
     * Returns null if null is passed in.
     */
    fun ulongToHex(value: Long?): String? {
        return if (value == null) {
            null
        } else if (value >= 0) {
            "0x%x".format(value)
        } else {
            return "0x%x%02x".format(value ushr 8, value and 0xff)
        }
    }

    /**
     * Convert a JSON-decoded value into a long. Accepts numeric types, or numeric encoded strings
     * (e.g. "1234", "0xb1ff").
     *
     * Returns null if null or an empty string is passed in.
     */
    fun jsonToLong(value: Any?): Long? {
        return when (value) {
            null -> null
            is Number -> value.toLong()
            is String -> {
                if (value.length == 0) {
                    null
                } else {
                    try {
                        java.lang.Long.decode(value)
                    } catch (e: NumberFormatException) {
                        // Check if the value overflows a long, and correct for it.
                        if (value.startsWith("0x")) {
                            // All problematic hex values (e.g. 0x8000000000000000) have 18 characters
                            if (value.length != 18) {
                                throw e
                            }
                            // Decode all but the last byte, then shift and add it.
                            // This overflows and gives the "correct" signed result.
                            val headLength = value.length - 2
                            java.lang.Long.decode(value.substring(0, headLength))
                                .shl(8)
                                .or(value.substring(headLength, value.length).toLong(16))
                        } else {
                            // The first problematic decimal value (9223372036854775808) has 19 digits
                            if (value.length < 19) {
                                throw e
                            }
                            // Decode all but the last 3 chars, then multiply and add them.
                            // This overflows and gives the "correct" signed result.
                            val headLength = value.length - 3
                            java.lang.Long.decode(value.substring(0, headLength)) *
                                    1000 +
                                    java.lang.Long.decode(value.substring(headLength, value.length))
                        }
                    }
                }
            }

            else -> throw IllegalArgumentException("Cannot convert $value to long")
        }
    }
}
