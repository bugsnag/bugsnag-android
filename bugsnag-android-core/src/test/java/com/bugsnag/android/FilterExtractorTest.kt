package com.bugsnag.android

import com.bugsnag.android.internal.JsonCollectionParser
import org.junit.Assert.assertEquals
import org.junit.Test

internal class FilterExtractorTest {
    val dataParser = JsonCollectionParser(
        """
            {
                "items": [
                    {"status": "active", "value": "item1"},
                    {"status": "inactive", "value": "item2"},
                    {"status": null, "value": "item3"},
                    {"value": "item4"}
                ]
            }
            """.byteInputStream()
    )

    @Suppress("UNCHECKED_CAST")
    val data = dataParser.parse() as Map<String, *>

    @Test
    fun testEquals() {
        // Test EQUALS match
        val equalJsonString = """{
            "path": "items.*",
            "pathMode": "FILTER",
            "filterPath": "status",
            "matchType": "EQUALS",
            "value": "active",
            "subPath": {"path": "value"}
        }"""
        val equalParser = JsonCollectionParser(equalJsonString.byteInputStream())

        @Suppress("UNCHECKED_CAST")
        val equalExtractor = JsonDataExtractor.fromJsonMap(equalParser.parse() as Map<String, *>)!!
        assertEquals(listOf("item1"), equalExtractor.extract(data))
    }

    @Test
    fun testNotEquals() {
        // Test NOT_EQUALS match
        val notEqualJsonString = """{
            "path": "items.*",
            "pathMode": "FILTER",
            "filterPath": "status",
            "matchType": "NOT_EQUALS",
            "value": "active",
            "subPath": {"path": "value"}
        }"""
        val notEqualParser = JsonCollectionParser(notEqualJsonString.byteInputStream())

        @Suppress("UNCHECKED_CAST")
        val notEqualExtractor =
            JsonDataExtractor.fromJsonMap(notEqualParser.parse() as Map<String, *>)!!
        assertEquals(listOf("item2", "item3", "item4"), notEqualExtractor.extract(data))
    }

    @Test
    fun testIsNull() {
        // Test IS_NULL match
        val isNullJsonString = """{
            "path": "items.*",
            "pathMode": "FILTER",
            "filterPath": "status",
            "matchType": "IS_NULL",
            "subPath": {"path": "value"}
        }"""
        val isNullParser = JsonCollectionParser(isNullJsonString.byteInputStream())

        @Suppress("UNCHECKED_CAST")
        val isNullExtractor =
            JsonDataExtractor.fromJsonMap(isNullParser.parse() as Map<String, *>)!!
        assertEquals(listOf("item3", "item4"), isNullExtractor.extract(data))
    }

    @Test
    fun testNotNull() {
        // Test NOT_NULL match
        val notNullJsonString = """{
            "path": "items.*",
            "pathMode": "FILTER",
            "filterPath": "status",
            "matchType": "NOT_NULL",
            "subPath": {"path": "value"}
        }"""
        val notNullParser = JsonCollectionParser(notNullJsonString.byteInputStream())

        @Suppress("UNCHECKED_CAST")
        val notNullExtractor =
            JsonDataExtractor.fromJsonMap(notNullParser.parse() as Map<String, *>)!!
        assertEquals(listOf("item1", "item2"), notNullExtractor.extract(data))
    }
}
