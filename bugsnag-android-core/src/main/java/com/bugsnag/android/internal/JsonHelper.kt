package com.bugsnag.android.internal

import com.dslplatform.json.DslJson
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

internal object JsonHelper {
    // Only one global DslJson is needed, and is thread-safe
    // Note: dsl-json adds about 150k to the final binary size.
    private val dslJson = DslJson<MutableMap<String, Any>>()

    fun serialize(value: Any, stream: OutputStream) {
        dslJson.serialize(value, stream)
    }

    fun serialize(value: Any, file: File) {
        val parentFile = file.parentFile
        if (parentFile != null && !parentFile.exists()) {
            if (!parentFile.mkdirs()) {
                throw FileSystemException(file, null, "Could not create parent dirs of file")
            }
        }
        try {
            file.outputStream().use { stream -> dslJson.serialize(value, stream) }
        } catch (ex: IOException) {
            throw IOException("Could not serialize JSON document to $file", ex)
        }
    }

    fun deserialize(bytes: ByteArray): MutableMap<String, Any> {
        val document = dslJson.deserialize(
            MutableMap::class.java,
            bytes,
            bytes.size
        )
        requireNotNull(document) { "JSON document is invalid" }
        @Suppress("UNCHECKED_CAST")
        return document as MutableMap<String, Any>
    }

    fun deserialize(stream: InputStream): MutableMap<String, Any> {
        val document = dslJson.deserialize(MutableMap::class.java, stream)
        requireNotNull(document) { "JSON document is invalid" }
        @Suppress("UNCHECKED_CAST")
        return document as MutableMap<String, Any>
    }

    fun deserialize(file: File): MutableMap<String, Any> {
        try {
            file.inputStream().use { stream -> return deserialize(stream) }
        } catch (ex: FileNotFoundException) {
            throw ex
        } catch (ex: IOException) {
            throw IOException("Could not deserialize from $file", ex)
        }
    }
}