package com.bugsnag.android

import com.bugsnag.android.internal.DateUtils
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.StringWriter
import java.util.Date

internal class RemoteConfigTest {
    private val remoteConfig = RemoteConfig(
        "tag123",
        Date(0),
        listOf()
    )

    @Test
    fun testToStreamWithEmptyDiscardRules() {
        val writer = StringWriter()
        val stream = JsonStream(writer)

        remoteConfig.toStream(stream)

        val json = writer.toString()
        assertTrue(json.contains("\"configurationTag\":\"tag123\""))
        assertTrue(json.contains("\"configExpiry\":\"${DateUtils.toIso8601(Date(0))}\""))
        assertTrue(json.contains("\"discardRules\":[]"))
    }

    @Test
    fun testToStreamWithDiscardRules() {
        val config = RemoteConfig(
            "tag456",
            Date(0),
            listOf(DiscardRule.All(), DiscardRule.AllHandled())
        )

        val writer = StringWriter()
        val stream = JsonStream(writer)

        config.toStream(stream)

        val json = writer.toString()
        assertTrue(json.contains("\"type\":\"all\""))
        assertTrue(json.contains("\"type\":\"allHandled\""))
    }
}
