package com.bugsnag.android.internal

import com.bugsnag.android.JsonStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.io.PrintWriter
import java.util.Date

internal object JsonHelper {

    fun serialize(streamable: JsonStream.Streamable): ByteArray {
        return ByteArrayOutputStream().use { baos ->
            PrintWriter(baos).use { pw ->
                JsonStream(pw).use(streamable::toStream)
                baos.toByteArray()
            }
        }
    }

    fun serialize(value: Any): ByteArray {
        return ByteArrayOutputStream().use { baos ->
            serialize(value, baos)
            baos.toByteArray()
        }
    }

    fun serialize(value: Any, stream: OutputStream) {
        stream.bufferedWriter().use { writer ->
            JsonStream(writer).use { jsonWriter ->
                writeValue(jsonWriter, value)
            }
        }
    }

    fun serialize(value: Any, file: File) {
        val parentFile = file.parentFile
        if (parentFile != null && !parentFile.exists()) {
            if (!parentFile.mkdirs()) {
                throw FileSystemException(file, null, "Could not create parent dirs of file")
            }
        }
        try {
            FileOutputStream(file).use { stream ->
                serialize(value, stream)
            }
        } catch (ex: IOException) {
            throw IOException("Could not serialize JSON document to $file", ex)
        }
    }

    /**
     * Deserialize JSON from byte array to Map/Collection objects
     */
    fun deserialize(data: ByteArray): Map<String, Any?> {
        return deserialize(ByteArrayInputStream(data))
    }

    /**
     * Deserialize JSON from InputStream to Map/Collection objects
     */
    fun deserialize(inputStream: InputStream): Map<String, Any?> {
        val parser = JsonCollectionParser(inputStream)
        val result = parser.parse()

        return when (result) {
            is Map<*, *> -> {
                @Suppress("UNCHECKED_CAST")
                result as Map<String, Any?>
            }
            else -> throw IllegalArgumentException("Expected JSON object at root level")
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
            return "0x%x%02x".format(value.ushr(8), value.and(0xff))
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

    @Suppress("UNCHECKED_CAST")
    private fun writeValue(writer: JsonStream, value: Any?) {
        when (value) {
            null -> writer.nullValue()
            is Map<*, *> -> {
                writer.beginObject()
                (value as Map<String, Any?>).forEach { (key, mapValue) ->
                    writer.name(key)
                    writeValue(writer, mapValue)
                }
                writer.endObject()
            }

            is Collection<*> -> {
                writer.beginArray()
                value.forEach { item ->
                    writeValue(writer, item)
                }
                writer.endArray()
            }

            is Boolean -> writer.value(value)
            is Number -> writer.value(value)
            is String -> writer.value(value)
            is Date -> writer.value(DateUtils.toIso8601(value))
            else -> writer.value(value.toString())
        }
    }
}
