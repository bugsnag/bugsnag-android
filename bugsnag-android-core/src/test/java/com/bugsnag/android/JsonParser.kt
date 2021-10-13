package com.bugsnag.android

import java.io.StringWriter

internal class JsonParser {

    fun read(resourceName: String): String {
        val resource = JsonParser::class.java.classLoader!!.getResource(resourceName)
            ?: throw NullPointerException("cannot read resource: '$resourceName'")
        return resource.readText()
    }

    fun toJsonString(streamable: JsonStream.Streamable): String {
        val writer = StringWriter()
        val jsonStream = JsonStream(writer)
        streamable.toStream(jsonStream)
        return writer.toString()
    }
}
