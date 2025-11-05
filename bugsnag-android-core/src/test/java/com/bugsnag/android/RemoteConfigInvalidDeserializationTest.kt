package com.bugsnag.android

import com.bugsnag.android.internal.JsonHelper
import org.junit.Assert.assertNull
import org.junit.Test

internal class RemoteConfigInvalidDeserializationTest {

    @Test
    fun testFromReaderMissingConfigurationExpiry() {
        val jsonString = """
        {
            "configurationTag": "tag123",
            "discardRules": []
        }
        """.trimIndent()

        val result = RemoteConfig.fromJsonMap(JsonHelper.deserialize(jsonString.toByteArray()))
        assertNull(result)
    }

    @Test
    fun testFromReaderMissingConfigurationTagAndExpiry() {
        val jsonString = """
        {
            "discardRules": []
        }
        """.trimIndent()

        val result = RemoteConfig.fromJsonMap(JsonHelper.deserialize(jsonString.toByteArray()))
        assertNull(result)
    }
}
