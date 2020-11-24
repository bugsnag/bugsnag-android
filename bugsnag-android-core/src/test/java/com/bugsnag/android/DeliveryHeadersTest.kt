package com.bugsnag.android

import com.bugsnag.android.BugsnagTestUtils.generateConfiguration
import com.bugsnag.android.BugsnagTestUtils.generateEventPayload
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.File
import java.util.Collections

class DeliveryHeadersTest {

    @Test
    fun verifyErrorApiHeaders() {
        val config = convertToImmutableConfig(generateConfiguration())
        val payload = generateEventPayload(config)
        val headers = config.getErrorApiDeliveryParams(payload).headers
        assertEquals(config.apiKey, headers["Bugsnag-Api-Key"])
        assertNotNull(headers["Bugsnag-Sent-At"])
        assertNotNull(headers["Bugsnag-Payload-Version"])
        assertNotNull(headers["Bugsnag-Stacktrace-Types"])
    }

    @Test
    fun verifyErrorApiHeadersDefaultStacktrace() {
        val config = convertToImmutableConfig(generateConfiguration())
        val payload = generateEventPayload(config)
        val headers = config.getErrorApiDeliveryParams(payload).headers
        assertEquals(config.apiKey, headers["Bugsnag-Api-Key"])
        assertNotNull(headers["Bugsnag-Sent-At"])
        assertEquals("4.0", headers["Bugsnag-Payload-Version"])
        assertEquals("android", headers["Bugsnag-Stacktrace-Types"])
    }

    @Test
    fun verifyErrorApiHeadersNoStacktrace() {
        val config = convertToImmutableConfig(generateConfiguration())
        val file = File("1504255147933_0000111122223333aaaabbbbcccc9999_my-uuid-123.json")
        val payload = EventPayload(config.apiKey, file, Notifier(), config)
        val headers = config.getErrorApiDeliveryParams(payload).headers
        assertNull(headers["Bugsnag-Stacktrace-Types"])
    }

    @Test
    fun verifyErrorApiHeadersMultiStacktrace() {
        val config = convertToImmutableConfig(generateConfiguration())
        val payload = generateEventPayload(config)

        // alter stacktrace to contain two types
        val error = requireNotNull(payload.event!!.errors[0])
        error.stacktrace[0].type = ErrorType.C
        error.stacktrace[1].type = ErrorType.REACTNATIVEJS

        val headers = config.getErrorApiDeliveryParams(payload).headers
        assertEquals("android,c,reactnativejs", headers["Bugsnag-Stacktrace-Types"])
    }
}
