package com.bugsnag.android

import com.bugsnag.android.internal.DateUtils
import com.squareup.moshi.JsonWriter
import java.io.IOException
import java.lang.reflect.Array
import java.util.Date

internal class MoshiObjectJsonStreamer {

    companion object {
        private const val REDACTED_PLACEHOLDER = "[REDACTED]"
        private const val OBJECT_PLACEHOLDER = "[OBJECT]"
    }

    var redactedKeys = setOf("password")

    // Write complex/nested values to a JsonStreamer
    @Throws(IOException::class)
    fun objectToStream(obj: Any?, writer: JsonWriter, shouldRedactKeys: Boolean = false) {
        when {
            obj == null -> writer.nullValue()
            obj is String -> writer.value(obj)
            obj is Number -> writer.value(obj)
            obj is Boolean -> writer.value(obj)
//            obj is JsonStream.Streamable -> obj.toStream(writer) TODO support me
            obj is Date -> writer.value(DateUtils.toIso8601(obj))
            obj is Map<*, *> -> mapToStream(writer, obj, shouldRedactKeys)
            obj is Collection<*> -> collectionToStream(writer, obj)
            obj.javaClass.isArray -> arrayToStream(writer, obj)
            else -> writer.value(OBJECT_PLACEHOLDER)
        }
    }

    private fun mapToStream(writer: JsonWriter, obj: Map<*, *>, shouldRedactKeys: Boolean) {
        writer.beginObject()
        obj.entries.forEach {
            val keyObj = it.key
            if (keyObj is String) {
                writer.name(keyObj)
                if (shouldRedactKeys && isRedactedKey(keyObj)) {
                    writer.value(REDACTED_PLACEHOLDER)
                } else {
                    objectToStream(it.value, writer, shouldRedactKeys)
                }
            }
        }
        writer.endObject()
    }

    private fun collectionToStream(writer: JsonWriter, obj: Collection<*>) {
        writer.beginArray()
        obj.forEach { objectToStream(it, writer) }
        writer.endArray()
    }

    private fun arrayToStream(writer: JsonWriter, obj: Any) {
        // Primitive array objects
        writer.beginArray()
        val length = Array.getLength(obj)
        var i = 0
        while (i < length) {
            objectToStream(Array.get(obj, i), writer)
            i += 1
        }
        writer.endArray()
    }

    // Should this key be redacted
    private fun isRedactedKey(key: String) = redactedKeys.any { key.contains(it) }
}
