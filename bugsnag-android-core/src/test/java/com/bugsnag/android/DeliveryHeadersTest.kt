package com.bugsnag.android

import com.bugsnag.android.BugsnagTestUtils.generateImmutableConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

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
}
