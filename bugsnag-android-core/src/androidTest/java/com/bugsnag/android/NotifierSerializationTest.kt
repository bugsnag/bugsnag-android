package com.bugsnag.android

import com.bugsnag.android.BugsnagTestUtils.streamableToJson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class NotifierSerializationTest {
    @Test
    fun testJsonSerialisation() {
        val notifierJson = streamableToJson(Notifier)
        assertEquals(3, notifierJson.length().toLong())
        assertEquals("Android Bugsnag Notifier", notifierJson.getString("name"))
        assertNotNull(notifierJson.getString("version"))
        assertEquals("https://bugsnag.com", notifierJson.getString("url"))
    }
}
