package com.bugsnag.android

import junit.framework.TestCase.assertNull
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

class EventErrorTypeTest {

    /**
     * An errortypes set contains every frame type included in the stacktrace
     */
    @Test
    fun verifyStacktraceErrorType() {
        val config = convertToImmutableConfig(BugsnagTestUtils.generateConfiguration())
        val payload = BugsnagTestUtils.generateEventPayload(config)
        assertEquals("android", payload.getErrorTypes())

        // alter stacktrace to contain two types
        val error = requireNotNull(payload.event!!.errors[0])
        error.stacktrace[0].type = ErrorType.C
        assertEquals("android,c", payload.getErrorTypes())

        error.stacktrace[0].type = ErrorType.REACTNATIVEJS
        assertEquals("android,reactnativejs", payload.getErrorTypes())
    }

    /**
     * Empty errortypes set is returned for a legacy filename
     */
    @Test
    fun verifyLegacyFileNameErrorType() {
        val config = convertToImmutableConfig(BugsnagTestUtils.generateConfiguration())
        val file = File("1504255147933_0000111122223333aaaabbbbcccc9999_my-uuid-123.json")
        val payload = EventPayload(config.apiKey, null, file, Notifier())
        assertNull(payload.getErrorTypes())
    }

    /**
     * An errortypes set contains every type included in the default filename
     */
    @Test
    fun verifyFileNameDefault() {
        val config = convertToImmutableConfig(BugsnagTestUtils.generateConfiguration())
        val file = File("1504255147933_0000111122223333aaaabbbbcccc9999_android_my-uuid-123_.json")
        val payload = EventPayload(config.apiKey, null, file, Notifier())
        assertEquals("android", payload.getErrorTypes())
    }

    /**
     * An errortypes set contains every type included in the default filename
     */
    @Test
    fun verifyFileNameMixedTypes() {
        val config = convertToImmutableConfig(BugsnagTestUtils.generateConfiguration())
        val file = File("1504255147933_0000111122223333aaaabbbbcccc9999_android,c,reactnativejs_my-uuid-123_.json")
        val payload = EventPayload(config.apiKey, null, file, Notifier())
        assertEquals("android,c,reactnativejs", payload.getErrorTypes())
    }
}
