package com.bugsnag.android

import com.bugsnag.android.internal.JsonCollectionParser
import org.junit.Assert.assertEquals
import org.junit.Test

internal class SimplePathExtractorTest {
    val dataParser = JsonCollectionParser(
        """{
            "user": {
                "profile": {
                    "email": "test@example.com",
                    "name": "Test User"
                }
            }
        }""".byteInputStream()
    )

    @Suppress("UNCHECKED_CAST")
    val data = dataParser.parse() as Map<String, *>

    @Test
    fun testSimplePathExtractorFromJson() {
        val jsonString = """{"path": "user.profile.email"}"""
        val parser = JsonCollectionParser(jsonString.byteInputStream())

        @Suppress("UNCHECKED_CAST")
        val extractorJson = parser.parse() as Map<String, *>

        val extractor = JsonDataExtractor.fromJsonMap(extractorJson)!!

        val results = extractor.extract(data)
        assertEquals(listOf("test@example.com"), results)
    }
}
