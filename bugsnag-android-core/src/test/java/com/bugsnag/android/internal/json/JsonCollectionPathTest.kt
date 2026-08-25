package com.bugsnag.android.internal.json

import com.bugsnag.android.internal.JsonCollectionParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

internal class JsonCollectionPathTest {
    @Test
    fun extractLastArrayElement() {
        val extracted = extractPathFromResource(
            "metaData.structuralTests.arrayTests.nested.*.-1",
            "event_serialization_9.json"
        )

        assertEquals(
            listOf(2L, listOf(4L, 5L), listOf(7L, listOf(8L, 9L))),
            extracted
        )
    }

    @Test
    fun extractFirstArrayElement() {
        val extracted = extractPathFromResource(
            "metaData.structuralTests.arrayTests.nested.*.0",
            "event_serialization_9.json"
        )

        assertEquals(
            listOf(1L, 3L, 6L),
            extracted
        )
    }

    @Test
    fun nonExistentPropertyPath() {
        val extracted = extractPathFromResource(
            "metaData.structuralTests.arrayTests.noValueHere",
            "event_serialization_9.json"
        )

        assertEquals(0, extracted.size)
    }

    @Test
    fun nonExistentNumericPath() {
        val extracted = extractPathFromResource(
            "metaData.structuralTests.arrayTests.0",
            "event_serialization_9.json"
        )

        assertEquals(0, extracted.size)
    }

    @Test
    fun nonExistentNegativeNumericPath() {
        val extracted = extractPathFromResource(
            "metaData.structuralTests.arrayTests.-1",
            "event_serialization_9.json"
        )

        assertEquals(0, extracted.size)
    }

    @Test
    fun numericMapKeys() {
        val numberKeys = extractPathFromResource("metaData.numbers.0", "path_fixture.json")
        assertEquals(listOf("naught"), numberKeys)
    }

    @Test
    fun wildcardArrayElements() {
        val numberKeys =
            extractPathFromResource("metaData.arrayOfObjects.*.name", "path_fixture.json")
        assertEquals(listOf("one", "two", "three"), numberKeys)
    }

    @Test
    fun wildcardObjectProperties() {
        val numberKeys = extractPathFromResource("metaData.numbers.*", "path_fixture.json")
        assertEquals(listOf("naught", "one", "two", "three"), numberKeys)
    }

    @Test
    fun toStringReturnsPath() {
        val path = "name.string.more words are here.0.-1.*.*.*.\uD83D\uDE00\uD83D\uDE03\uD83D\uDE04"
        val json = JsonCollectionPath.fromString(path)

        assertEquals(path, json.toString())
    }

    private fun extractPathFromResource(path: String, resource: String): List<Any> {
        val json = JsonCollectionParser(this::class.java.getResourceAsStream("/$resource")!!)
            .parse()

        val collectionPath = JsonCollectionPath.fromString(path)
        assertNotNull("path failed to parse: '$path'", collectionPath)

        @Suppress("UNCHECKED_CAST")
        return collectionPath.extractFrom(json as Map<String, *>)
    }
}
