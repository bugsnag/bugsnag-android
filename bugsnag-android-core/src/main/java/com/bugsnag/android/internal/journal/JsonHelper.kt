package com.bugsnag.android.internal.journal

import com.bugsnag.android.JsonReader
import com.bugsnag.android.JsonToken
import com.bugsnag.android.JsonStream
import com.bugsnag.android.ObjectJsonStreamer
import com.dslplatform.json.DslJson
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.lang.NumberFormatException

class JsonHelper private constructor() {

    companion object {
        fun serialize(value: Any, stream: OutputStream) {
            val writer = JsonStream(stream.bufferedWriter()).apply {
                serializeNulls = true // TODO check necessary?
            }
            val jsonStreamer = ObjectJsonStreamer()
            jsonStreamer.objectToStream(value, writer, false)
            writer.flush()
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

        fun deserialize(stream: InputStream): MutableMap<in String, out Any> {
            return convertJsonToMap(stream)
        }

        fun deserialize(bytes: ByteArray): MutableMap<String, Any> {
            val stream = ByteArrayInputStream(bytes)
            @Suppress("UNCHECKED_CAST")
            return deserialize(stream) as MutableMap<String, Any>
        }

        fun deserialize(file: File): MutableMap<in String, out Any> {
            try {
                FileInputStream(file).use { stream -> return deserialize(stream) }
            } catch (ex: FileNotFoundException) {
                throw ex
            } catch (ex: IOException) {
                throw IOException("Could not deserialize from $file", ex)
            }
        }

        private fun convertJsonToMap(stream: InputStream): MutableMap<String, Any> {
            return JsonReader(stream.bufferedReader()).use { reader ->
                reader.nextObject()
            }
        }

        private fun JsonReader.nextValue(): Any = when (val token = peek()) {
            JsonToken.NULL -> nextNull()
            JsonToken.BOOLEAN -> nextBoolean()
            JsonToken.STRING -> nextString()
            JsonToken.NUMBER -> nextNumber()
            JsonToken.BEGIN_ARRAY -> nextArray()
            JsonToken.BEGIN_OBJECT -> nextObject()
            else -> throw IllegalStateException("Unexpected state $token")
        }

        private fun JsonReader.nextArray(): MutableList<Any> {
            val list = mutableListOf<Any>()
            beginArray()
            while (hasNext()) {
                val value = nextValue()
                list.add(value)
            }
            endArray()
            return list
        }

        private fun JsonReader.nextObject(): MutableMap<String, Any> {
            val map = mutableMapOf<String, Any>()
            beginObject()
            while (hasNext()) {
                val name = nextName()
                val value = nextValue()
                map[name] = value
            }
            endObject()
            return map
        }

        private fun JsonReader.nextNumber(): Number {
            return try {
                 nextInt()
            } catch (exc: NumberFormatException) {
                try {
                    nextLong()
                } catch (exc: java.lang.IllegalStateException) {
                    nextDouble()
                }
            }
//            return nextInt() // TODO need to support long + double.
        }
    }
}
