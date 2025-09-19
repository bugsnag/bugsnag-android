package com.bugsnag.android

import android.util.JsonReader
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.StringReader

internal class RemoteConfigDeserializationTest {

    @Test
    fun testFromReaderValidJson() {
        val validJson = """
    {
        "configurationTag": "tag123",
        "configurationExpiry": "2024-01-15T10:30:45.123Z",
        "discardRules": [
            {
                "match_type": "ALL"
            },
            {
                "match_type": "ALL_HANDLED"
            }
        ]
    }
        """.trimIndent()

        val reader = JsonReader(StringReader(validJson))
        assertNotNull(reader)
        val result = RemoteConfig.fromReader(reader)
        assertNotNull(result)
    }

    @Test
    fun testFromReaderMissingConfigurationTag() {
        val jsonString = """
        {
            "configurationExpiry": "2024-01-15T10:30:45.123Z",
            "discardRules": []
        }
        """.trimIndent()

        val reader = JsonReader(StringReader(jsonString))
        val result = RemoteConfig.fromReader(reader)

        assertNull(result)
    }

    @Test
    fun testFromReaderMissingConfigurationExpiry() {
        val jsonString = """
        {
            "configurationTag": "tag123",
            "discardRules": []
        }
        """.trimIndent()

        val reader = JsonReader(StringReader(jsonString))
        val result = RemoteConfig.fromReader(reader)

        assertNull(result)
    }

    @Test
    fun testFromReaderMissingConfigurationTagAndExpiry() {
        val jsonString = """
        {
            "discardRules": []
        }
        """.trimIndent()

        val reader = JsonReader(StringReader(jsonString))
        val result = RemoteConfig.fromReader(reader)

        assertNull(result)
    }
}
