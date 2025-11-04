package com.bugsnag.android

import com.bugsnag.android.internal.JsonCollectionParser
import org.junit.Assert.assertEquals
import org.junit.Test

internal class RelativeAddressExtractorTest {
    val dataParser = JsonCollectionParser(
        """{
            "frames": [
                {"frameAddress": "0x1000", "loadAddress": "0x800"},
                {"frameAddress": "0x2000", "machoLoadAddress": "0x1000"},
                {"frameAddress": "4096", "loadAddress": "2048"},
                {"frameAddress": "invalid", "loadAddress": "0x800"}
            ]
        }""".byteInputStream()
    )

    @Suppress("UNCHECKED_CAST")
    val data = dataParser.parse() as Map<String, *>

    @Test
    fun testRelativeAddressExtractorFromJson() {
        val jsonString = """{"path": "frames.*", "pathMode": "RELATIVE_ADDRESS"}"""
        val parser = JsonCollectionParser(jsonString.byteInputStream())

        @Suppress("UNCHECKED_CAST")
        val extractorJson = parser.parse() as Map<String, *>

        val extractor = JsonDataExtractor.fromJsonMap(extractorJson)!!

        val results = extractor.extract(data)
        assertEquals(listOf("0x800", "0x1000", "0x800"), results)
    }
}
