package com.bugsnag.android

import org.junit.Assert
import java.io.StringWriter

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

private fun validateJson(resourceName: String, json: String) {
    val whitespace = "\\s".toRegex()
    val expectedJson = JsonParser().read(resourceName).replace(whitespace, "")
    val generatedJson = json.replace(whitespace, "")
    Assert.assertEquals(expectedJson, generatedJson)
}

/**
 * Generates parameterised test cases from a variable number of [JsonStream.Streamable] elements.
 * The expected JSON file for each element should match the naming format
 * '$filename_serialization_$index.json'
 */
internal fun <T> generateTestCases(
    filename: String,
    vararg elements: T
): Collection<Pair<T, String>> {
    return elements.mapIndexed { index, obj ->
        Pair(obj, "${filename}_serialization_$index.json")
    }
}
