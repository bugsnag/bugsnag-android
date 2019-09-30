package com.bugsnag.android

import org.junit.Assert

/**
 * Serializes a [JsonStream.Streamable] object into JSON and compares its equality against a JSON
 * test fixture loaded from resources
 */
internal fun verifyJsonMatches(streamable: JsonStream.Streamable, resourceName: String) {
    val jsonParser = JsonParser()
    val whitespace = "\\s".toRegex()
    val expectedJson = jsonParser.read(resourceName).replace(whitespace, "")
    val generatedJson = jsonParser.toJsonString(streamable).replace(whitespace, "")
    Assert.assertEquals(expectedJson, generatedJson)
}

/**
 * Generates parameterised test cases from a variable number of [JsonStream.Streamable] elements.
 * The expected JSON file for each element should match the naming format
 * '$filename_serialization_$index.json'
 */
internal fun generateTestCases(
    filename: String,
    vararg elements: JsonStream.Streamable
): Collection<Pair<JsonStream.Streamable, String>> {
    return elements.mapIndexed { index, obj ->
        Pair(obj, "${filename}_serialization_$index.json")
    }
}
