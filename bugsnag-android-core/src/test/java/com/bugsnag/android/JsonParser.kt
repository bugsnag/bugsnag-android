package com.bugsnag.android

import java.io.BufferedReader
import java.io.StringReader
import java.io.StringWriter

internal class JsonParser {

    fun read(resourceName: String): String {
        return JsonParser::class.java.classLoader!!.getResource(resourceName).readText()
    }

    fun parse(resourceName: String): JsonReader {
        val json = read(resourceName)
        return JsonReader(BufferedReader(StringReader(json)))
    }

    fun toJsonString(streamable: JsonStream.Streamable): String {
        val writer = StringWriter()
        val jsonStream = JsonStream(writer)
        streamable.toStream(jsonStream)
        return writer.toString()
    }

}

