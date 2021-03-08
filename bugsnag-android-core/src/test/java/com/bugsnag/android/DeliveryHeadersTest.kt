package com.bugsnag.android

import com.bugsnag.android.BugsnagTestUtils.generateConfiguration
import com.bugsnag.android.BugsnagTestUtils.generateEventPayload
import com.bugsnag.android.BugsnagTestUtils.generateImmutableConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class DeliveryHeadersTest {

    private val sha1Regex = "sha1 [0-9a-f]{40}".toRegex()

    @Test
    fun computeSha1Digest() {
        val payload = generateEventPayload(generateImmutableConfig())
        val payload1 = serializeJsonPayload(payload)
        val firstSha = requireNotNull(computeSha1Digest(payload1))
        val payload2 = serializeJsonPayload(payload)
        val secondSha = requireNotNull(computeSha1Digest(payload2))

        // the hash equals the expected value
        assertTrue(firstSha.matches(sha1Regex))

        // the hash is consistent
        assertEquals(firstSha, secondSha)

        // altering the streamable alters the hash
        payload.event!!.device.id = "50923"
        val payload3 = serializeJsonPayload(payload)
        val differentSha = requireNotNull(computeSha1Digest(payload3))
        assertNotEquals(firstSha, differentSha)
        assertTrue(differentSha.matches(sha1Regex))
    }

    @Test
    fun verifyErrorApiHeaders() {
        val config = convertToImmutableConfig(generateConfiguration())
        val payload = generateEventPayload(config)
        val headers = config.getErrorApiDeliveryParams(payload).headers
        assertEquals(config.apiKey, headers["Bugsnag-Api-Key"])
        assertEquals("application/json", headers["Content-Type"])
        assertNotNull(headers["Bugsnag-Sent-At"])
        assertNotNull(headers["Bugsnag-Payload-Version"])
        assertNotNull(headers["Bugsnag-Stacktrace-Types"])
    }

    @Test
    fun verifySessionApiHeaders() {
        val config = generateImmutableConfig()
        val headers = config.getSessionApiDeliveryParams().headers
        assertEquals(config.apiKey, headers["Bugsnag-Api-Key"])
        assertEquals("application/json", headers["Content-Type"])
        assertNotNull(headers["Bugsnag-Sent-At"])
        assertNotNull(headers["Bugsnag-Payload-Version"])
        assertNull(headers["Bugsnag-Stacktrace-Types"])
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
        val payload = EventPayload(config.apiKey, null, file, Notifier(), config)
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
