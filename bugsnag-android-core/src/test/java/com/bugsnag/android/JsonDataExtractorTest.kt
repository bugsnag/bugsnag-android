package com.bugsnag.android

import com.bugsnag.android.internal.JsonCollectionParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

internal class JsonDataExtractorTest {
    // Parse sample data using JsonCollectionParser
    val sampleDataParser = JsonCollectionParser(
        """{
            "user": {
                "name": "john_doe",
                "id": 12345
            },
            "errors": [
                {"message": "Error: 404 - Not Found"},
                {"message": "Error: 500 - Internal Server Error"},
                {"message": "Warning: Something happened"}
            ],
            "breadcrumbs": [
                {"type": "navigation", "name": "MainActivity"},
                {"type": "user", "name": "button_click"},
                {"type": "navigation", "name": "SettingsActivity"}
            ],
            "stacktrace": [
                {"method": "onCreate", "file": "MainActivity.kt", "line": 42},
                {"method": "onResume", "file": null},
                {"method": "onPause", "file": "BaseActivity.kt"}
            ],
            "stackframes": [
                {"frameAddress": "0x1000", "loadAddress": "0x800", "method": "main"},
                {"frameAddress": "0x2000", "machoLoadAddress": "0x1500", "method": "init"}
            ]
        }""".byteInputStream()
    )

    @Suppress("UNCHECKED_CAST")
    val sampleData = sampleDataParser.parse() as Map<String, *>

    @Test
    fun testAllExtractorTypesFromJson() {
        // Define extractors as raw JSON strings (as they would be loaded from configuration)
        val extractorJsonStrings = listOf(
            // SimplePathExtractor
            """{"path": "user.name"}""",
            // RegexExtractor
            """{"path": "errors.*.message", "pathMode": "REGEX", "regex": "Error: (\\d+) - (.+)"}""",
            // FilterExtractor with EQUAL match
            """{
                "path": "breadcrumbs.*",
                "pathMode": "FILTER",
                "filter": {
                    "conditions": [
                        {
                            "filterPath": { "path": "type" },
                            "matchType": "EQUALS",
                            "value": "navigation"
                        }
                    ],
                    "subPaths": [
                        { "path": "name" }
                    ]
                }
            }""",
            // FilterExtractor with "not null" match
            """{
                "path": "stacktrace.*",
                "pathMode": "FILTER",
                "filter": {
                    "conditions": [
                        {
                            "filterPath": { "path": "file" },
                            "matchType": "IS_NULL",
                            "value": "false"
                        }
                    ],
                    "subPaths": [
                        { "path": "method" }
                    ]
                }
            }""",
            // RelativeAddressExtractor
            """{"path": "stackframes.*", "pathMode": "RELATIVE_ADDRESS"}"""
        )

        // Parse JSON strings into Map structures using JsonCollectionParser
        val extractorJsons = extractorJsonStrings.map { jsonString ->
            val parser = JsonCollectionParser(jsonString.byteInputStream())
            @Suppress("UNCHECKED_CAST")
            parser.parse() as Map<String, *>
        }

        // Create extractors from parsed JSON
        val extractors = JsonDataExtractor.fromJsonList(extractorJsons)
        assertEquals(5, extractors.size)

        // Test each extractor
        val results = extractors.map { extractor ->
            extractor.extract(sampleData)
        }

        // Verify SimplePathExtractor results
        assertEquals(listOf("john_doe"), results[0])

        // Verify RegexExtractor results
        assertEquals(listOf("404,Not Found", "500,Internal Server Error"), results[1])

        // Verify FilterExtractor with EQUAL match results
        assertEquals(listOf("MainActivity", "SettingsActivity"), results[2])

        // Verify FilterExtractor with NOT_NULL match results
        assertEquals(listOf("onCreate", "onPause"), results[3])

        // Verify RelativeAddressExtractor results
        assertEquals(listOf("0x800", "0xb00"), results[4])
    }

    @Test
    fun testInvalidDataExtractorConfig() {
        // Test invalid path mode
        val invalidJson1String = """{"path": "test.path", "pathMode": "INVALID_MODE"}"""
        val parser1 = JsonCollectionParser(invalidJson1String.byteInputStream())
        @Suppress("UNCHECKED_CAST")
        assertNull(JsonDataExtractor.fromJsonMap(parser1.parse() as Map<String, *>))

        // Test missing path
        val invalidJson2String = """{"pathMode": "REGEX"}"""
        val parser2 = JsonCollectionParser(invalidJson2String.byteInputStream())
        @Suppress("UNCHECKED_CAST")
        assertNull(JsonDataExtractor.fromJsonMap(parser2.parse() as Map<String, *>))

        // Test invalid regex extractor (missing regex)
        val invalidJson3String = """{"path": "test.path", "pathMode": "REGEX"}"""
        val parser3 = JsonCollectionParser(invalidJson3String.byteInputStream())
        @Suppress("UNCHECKED_CAST")
        assertNull(JsonDataExtractor.fromJsonMap(parser3.parse() as Map<String, *>))

        // Test invalid filter extractor (missing filterPath)
        val invalidJson4String =
            """{"path": "test.path", "pathMode": "FILTER", "matchType": "EQUAL"}"""
        val parser4 = JsonCollectionParser(invalidJson4String.byteInputStream())
        @Suppress("UNCHECKED_CAST")
        assertNull(JsonDataExtractor.fromJsonMap(parser4.parse() as Map<String, *>))
    }
}
