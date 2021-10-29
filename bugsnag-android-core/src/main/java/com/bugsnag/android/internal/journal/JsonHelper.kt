package com.bugsnag.android.internal.journal

import com.bugsnag.android.MoshiObjectJsonStreamer
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import okio.BufferedSource
import okio.buffer
import okio.sink
import okio.source
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

class JsonHelper private constructor() {

    companion object {
        fun serialize(value: Any, stream: OutputStream) {
            stream.sink().buffer().use { sink ->
                val writer = JsonWriter.of(sink).apply {
                    serializeNulls = true
                }
                val jsonStreamer = MoshiObjectJsonStreamer()
                jsonStreamer.objectToStream(value, writer, false)
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
            val src: BufferedSource = stream.source().buffer()
            return JsonReader.of(src).use { reader ->
                reader.nextObject()
            }
        }

        private fun JsonReader.nextValue(): Any = when (val token = peek()) {
            JsonReader.Token.NULL -> nextNull()
            JsonReader.Token.BOOLEAN -> nextBoolean()
            JsonReader.Token.STRING -> nextString()
            JsonReader.Token.NUMBER -> nextNumber()
            JsonReader.Token.BEGIN_ARRAY -> nextArray()
            JsonReader.Token.BEGIN_OBJECT -> nextObject()
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
