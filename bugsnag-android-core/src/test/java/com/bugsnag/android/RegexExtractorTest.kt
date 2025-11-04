package com.bugsnag.android

import com.bugsnag.android.internal.JsonCollectionParser
import org.junit.Assert.assertEquals
import org.junit.Test

internal class RegexExtractorTest {
    val dataParser = JsonCollectionParser(
        """
        {
            "logs": [
                "[ERROR] Database connection failed",
                "[INFO] Application started",
                "No match here"
            ]
        }""".byteInputStream()
    )

    @Suppress("UNCHECKED_CAST")
    val data = dataParser.parse() as Map<String, *>

    @Test
    fun testRegexExtractorWithGroupsFromJson() {
        val jsonString = """{"path": "logs.*", "pathMode": "REGEX", "regex": "\\[(\\w+)\\] (.+)"}"""
        val parser = JsonCollectionParser(jsonString.byteInputStream())

        @Suppress("UNCHECKED_CAST")
        val extractorJson = parser.parse() as Map<String, *>

        val extractor = JsonDataExtractor.fromJsonMap(extractorJson)!!

        val results = extractor.extract(data)
        assertEquals(
            listOf("ERROR,Database connection failed", "INFO,Application started"),
            results
        )
    }
}
