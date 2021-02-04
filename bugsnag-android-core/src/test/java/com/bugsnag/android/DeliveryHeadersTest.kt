package com.bugsnag.android

import com.bugsnag.android.BugsnagTestUtils.generateImmutableConfig
import org.junit.Assert
import org.junit.Assert.*
import org.junit.Test
import java.io.BufferedWriter
import java.io.StringWriter
import java.util.Date

class DeliveryHeadersTest {

    private val sha1Regex = "sha1 [0-9a-f]{40}".toRegex()

    @Test
    fun computeSha1Digest() {
        val payload = BugsnagTestUtils.generateEventPayload(generateImmutableConfig())
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
        val config = convertToImmutableConfig(BugsnagTestUtils.generateConfiguration())
        val payload = BugsnagTestUtils.generateEventPayload(config)
        val headers = config.getErrorApiDeliveryParams(payload).headers
        assertEquals(config.apiKey, headers["Bugsnag-Api-Key"])
        assertEquals("application/json", headers["Content-Type"])
        assertNotNull(headers["Bugsnag-Sent-At"])
        assertNotNull(headers["Bugsnag-Payload-Version"])
    }

    @Test
    fun verifySessionApiHeaders() {
        val config = generateImmutableConfig()
        val headers = config.getSessionApiDeliveryParams().headers
        assertEquals(config.apiKey, headers["Bugsnag-Api-Key"])
        assertEquals("application/json", headers["Content-Type"])
        assertNotNull(headers["Bugsnag-Sent-At"])
        assertNotNull(headers["Bugsnag-Payload-Version"])
    }
}
