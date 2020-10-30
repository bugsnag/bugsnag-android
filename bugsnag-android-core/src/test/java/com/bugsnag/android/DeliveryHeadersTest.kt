package com.bugsnag.android

import com.bugsnag.android.BugsnagTestUtils.generateImmutableConfig
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date

class DeliveryHeadersTest {

    private val sha1Regex = "sha1 [0-9a-f]{40}".toRegex()

    @Test
    fun computeSha1Digest() {
        val payload = BugsnagTestUtils.generateEventPayload(generateImmutableConfig())
        val firstSha = requireNotNull(computeSha1Digest(payload))
        val secondSha = requireNotNull(computeSha1Digest(payload))

        // the hash equals the expected value
        assertTrue(firstSha.matches(sha1Regex))

        // the hash is consistent
        assertEquals(firstSha, secondSha)

        // altering the streamable alters the hash
        payload.event!!.device.id = "50923"
        val differentSha = requireNotNull(computeSha1Digest(payload))
        assertNotEquals(firstSha, differentSha)
        assertTrue(differentSha.matches(sha1Regex))
    }

    @Test
    fun verifyErrorApiHeaders() {
        val config = convertToImmutableConfig(BugsnagTestUtils.generateConfiguration())
        val payload = BugsnagTestUtils.generateEventPayload(config)
        val headers = config.getErrorApiDeliveryParams(payload).headers
        assertEquals(config.apiKey, headers["Bugsnag-Api-Key"])
        Assert.assertNotNull(headers["Bugsnag-Sent-At"])
        Assert.assertNotNull(headers["Bugsnag-Payload-Version"])

        val integrity = requireNotNull(headers["Bugsnag-Integrity"])
        assertTrue(integrity.matches(sha1Regex))
    }

    @Test
    fun verifySessionApiHeaders() {
        val config = generateImmutableConfig()
        val user = User("123", "hi@foo.com", "Li")
        val session = Session("abc", Date(0), user, 1, 0, Notifier(), NoopLogger)
        val headers = config.getSessionApiDeliveryParams(session).headers
        assertEquals(config.apiKey, headers["Bugsnag-Api-Key"])
        Assert.assertNotNull(headers["Bugsnag-Sent-At"])
        Assert.assertNotNull(headers["Bugsnag-Payload-Version"])

        val integrity = requireNotNull(headers["Bugsnag-Integrity"])
        assertTrue(integrity.matches(sha1Regex))
    }
}
