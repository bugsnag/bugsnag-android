package com.bugsnag.android

import com.bugsnag.android.internal.JsonCollectionParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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
            "filter": {
              "conditions": [
                {
                  "filterPath": { "path": "status" },
                  "matchType": "EQUALS",
                  "value": "active"
                }
              ],
              "subPaths": [
                { "path": "value" }                
              ]
            }
        }"""
        val equalParser = JsonCollectionParser(equalJsonString.byteInputStream())

        @Suppress("UNCHECKED_CAST")
        val equalsExtractor = JsonDataExtractor.fromJsonMap(equalParser.parse() as Map<String, *>)
        assertNotNull("FilterExtractor did not parse", equalsExtractor)
        assertEquals(listOf("item1"), equalsExtractor!!.extract(data))
    }

    @Test
    fun testNotEquals() {
        // Test NOT_EQUALS match
        val notEqualJsonString = """{
            "path": "items.*",
            "pathMode": "FILTER",
            "filter": {
              "conditions": [
                {
                  "filterPath": { "path": "status" },
                  "matchType": "NOT_EQUALS",
                  "value": "active"
                }              
              ],                
              "subPaths": [
                { "path": "value" }              
              ]
            }
        }"""
        val notEqualParser = JsonCollectionParser(notEqualJsonString.byteInputStream())

        @Suppress("UNCHECKED_CAST")
        val notEqualExtractor =
            JsonDataExtractor.fromJsonMap(notEqualParser.parse() as Map<String, *>)
        assertNotNull("FilterExtractor did not parse", notEqualExtractor)
        assertEquals(listOf("item2", "item3", "item4"), notEqualExtractor!!.extract(data))
    }

    @Test
    fun testIsNull() {
        // Test IS_NULL match
        val isNullJsonString = """{
            "path": "items.*",
            "pathMode": "FILTER",
            "filter": {
              "conditions": [
                {
                  "filterPath": { "path": "status" },
                  "matchType": "IS_NULL",
                  "value": "true"
                }
              ],
              "subPaths": [
                { "path": "value" }              
              ]
            }
        }"""
        val isNullParser = JsonCollectionParser(isNullJsonString.byteInputStream())

        @Suppress("UNCHECKED_CAST")
        val isNullExtractor =
            JsonDataExtractor.fromJsonMap(isNullParser.parse() as Map<String, *>)
        assertNotNull("FilterExtractor did not parse", isNullExtractor)
        assertEquals(listOf("item3", "item4"), isNullExtractor!!.extract(data))
    }

    @Test
    fun testNotNull() {
        // Test NOT_NULL match
        val notNullJsonString = """{
            "path": "items.*",
            "pathMode": "FILTER",
            "filter": {
              "conditions": [
                {
                    "filterPath": { "path": "status" },
                    "matchType": "IS_NULL",
                    "value": "false"
                }
              ],
              "subPaths": [ { "path": "value" } ]
            }
        }"""
        val notNullParser = JsonCollectionParser(notNullJsonString.byteInputStream())

        @Suppress("UNCHECKED_CAST")
        val notNullExtractor =
            JsonDataExtractor.fromJsonMap(notNullParser.parse() as Map<String, *>)
        assertNotNull("FilterExtractor did not parse", notNullExtractor)
        assertEquals(listOf("item1", "item2"), notNullExtractor!!.extract(data))
    }
}
