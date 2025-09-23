package com.bugsnag.android

import android.util.JsonReader
import com.bugsnag.android.internal.JsonCollectionParser
import com.bugsnag.android.internal.JsonHelper
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.StringReader

internal class RemoteConfigInvalidDeserializationTest {

    @Test
    fun testFromReaderMissingConfigurationTag() {
        val jsonString = """
        {
            "configurationExpiry": "2024-01-15T10:30:45.123Z",
            "discardRules": []
        }
        """.trimIndent()

        val result = RemoteConfig.fromMap(JsonHelper.deserialize(jsonString.toByteArray()))
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

        val result = RemoteConfig.fromMap(JsonHelper.deserialize(jsonString.toByteArray()))
        assertNull(result)
    }

    @Test
    fun testFromReaderMissingConfigurationTagAndExpiry() {
        val jsonString = """
        {
            "discardRules": []
        }
        """.trimIndent()

        val result = RemoteConfig.fromMap(JsonHelper.deserialize(jsonString.toByteArray()))
        assertNull(result)
    }
}
