package com.bugsnag.android

import java.io.StringWriter

internal class JsonParser {

    fun read(resourceName: String): String {
        return JsonParser::class.java.classLoader!!.getResource(resourceName).readText()
    }

    fun toJsonString(streamable: JsonStream.Streamable): String {
        val writer = StringWriter()
        val jsonStream = JsonStream(writer)
        streamable.toStream(jsonStream)
        return writer.toString()
    }
}
