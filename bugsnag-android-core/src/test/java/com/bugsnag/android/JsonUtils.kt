package com.bugsnag.android

import com.bugsnag.android.internal.JsonHelper
import org.junit.Assert
import java.io.StringWriter
import java.lang.NullPointerException

/**
 * Serializes a [JsonStream.Streamable] object into JSON and compares its equality against a JSON
 * test fixture loaded from resources
 */
internal fun verifyJsonMatches(streamable: JsonStream.Streamable, resourceName: String) {
    validateJson(resourceName, JsonParser().toJsonString(streamable))
}

internal fun verifyJsonMatches(map: Map<String, Any>, resourceName: String) {
    val stringWriter = StringWriter()
    val jsonStream = JsonStream(stringWriter)
    ObjectJsonStreamer().objectToStream(map, jsonStream)
    val json = stringWriter.toString()
    validateJson(resourceName, json)
}

internal fun validateJson(resourceName: String, json: String) {
    val whitespace = "\\s".toRegex()
    val rawJson = JsonParser().read(resourceName)
    val expectedJson = rawJson.replace(whitespace, "")
    val generatedJson = json.replace(whitespace, "")
    Assert.assertEquals(expectedJson, generatedJson)
}

@Suppress("UNCHECKED_CAST")
internal fun verifyJsonParser(
    streamable: JsonStream.Streamable,
    resourceName: String,
    parse: (MutableMap<String, Any?>) -> JsonStream.Streamable
) {
    val expectedJson = JsonParser().toJsonString(streamable)
    val resourceStream = JsonParser::class.java.classLoader?.getResourceAsStream(resourceName)
        ?: throw NullPointerException("cannot find resource: '$resourceName'")
    val loadedObject = parse(JsonHelper.deserialize(resourceStream) as MutableMap<String, Any?>)
    val generatedJson = JsonParser().toJsonString(loadedObject)
    Assert.assertEquals(expectedJson, generatedJson)
}

/**
 * Generates parameterised test cases from a variable number of [JsonStream.Streamable] elements.
 * The expected JSON file for each element should match the naming format
 * '$filename_serialization_$index.json'
 */
internal fun <T> generateSerializationTestCases(
    filename: String,
    vararg elements: T
): Collection<Pair<T, String>> {
    return elements.mapIndexed { index, obj ->
        Pair(obj, "${filename}_serialization_$index.json")
    }
}
