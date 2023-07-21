package com.bugsnag.android

import com.bugsnag.android.ErrorType.ANDROID
import com.bugsnag.android.ErrorType.C
import com.bugsnag.android.ErrorType.REACTNATIVEJS
import com.bugsnag.android.internal.convertToImmutableConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
        assertEquals(setOf(ANDROID), payload.getErrorTypes())

        // alter stacktrace to contain two types
        val error = requireNotNull(payload.event!!.errors[0])
        error.stacktrace[0].type = C
        assertEquals(setOf(ANDROID, C), payload.getErrorTypes())

        error.stacktrace[0].type = REACTNATIVEJS
        assertEquals(setOf(ANDROID, REACTNATIVEJS), payload.getErrorTypes())
    }

    /**
     * If only one error type is in the stackframes, the errorType is not set on the individual
     * frames.
     */
    @Test
    fun testNormalizeSingleErrorType() {
        val config = convertToImmutableConfig(BugsnagTestUtils.generateConfiguration())
        val payload = BugsnagTestUtils.generateEventPayload(config)
        assertEquals(setOf(ANDROID), payload.getErrorTypes())

        // alter stacktrace to contain one type
        val event = checkNotNull(payload.event)
        val error = checkNotNull(event.errors.single())
        error.stacktrace.forEach { it.type = ANDROID }

        // verify type was set appropriately
        assertEquals(ANDROID, error.type)
        assertEquals(setOf(ANDROID), payload.getErrorTypes())

        // confirm single type is stripped from individual stackframes
        event.impl.normalizeStackframeErrorTypes()
        assertEquals(ANDROID, error.type)
        assertEquals(setOf(ANDROID), payload.getErrorTypes())
        error.stacktrace.forEach { assertNull(it.type) }
    }

    /**
     * If multiple error types are in the stackframes, the errorType is set on the individual
     * frames.
     */
    @Test
    fun testNormalizeMultipleErrorType() {
        val config = convertToImmutableConfig(BugsnagTestUtils.generateConfiguration())
        val payload = BugsnagTestUtils.generateEventPayload(config)
        assertEquals(setOf(ANDROID), payload.getErrorTypes())

        // alter stacktrace to contain two types
        val event = checkNotNull(payload.event)
        val error = checkNotNull(event.errors.single())
        error.stacktrace.forEachIndexed { index, frame ->
            frame.type = when {
                index % 2 == 0 -> ANDROID
                else -> C
            }
        }

        // verify type was set appropriately
        assertEquals(ANDROID, error.type)
        assertEquals(setOf(ANDROID, C), payload.getErrorTypes())

        // confirm single type is stripped from individual stackframes
        event.impl.normalizeStackframeErrorTypes()
        assertEquals(ANDROID, error.type)
        assertEquals(setOf(ANDROID, C), payload.getErrorTypes())

        error.stacktrace.forEachIndexed { index, frame ->
            when {
                index % 2 == 0 -> assertEquals(ANDROID, frame.type)
                else -> assertEquals(C, frame.type)
            }
        }
    }

    /**
     * Empty errortypes set is returned for a legacy filename
     */
    @Test
    fun verifyLegacyFileNameErrorType() {
        val config = convertToImmutableConfig(BugsnagTestUtils.generateConfiguration())
        val file = File("1504255147933_0000111122223333aaaabbbbcccc9999_my-uuid-123.json")
        val payload = EventPayload(config.apiKey, null, file, Notifier(), config)
        assertEquals(emptySet<ErrorType>(), payload.getErrorTypes())
    }

    /**
     * An errortypes set contains every type included in the default filename
     */
    @Test
    fun verifyFileNameDefault() {
        val config = convertToImmutableConfig(BugsnagTestUtils.generateConfiguration())
        val file = File("1504255147933_0000111122223333aaaabbbbcccc9999_android_my-uuid-123_.json")
        val payload = EventPayload(config.apiKey, null, file, Notifier(), config)
        assertEquals(setOf(ANDROID), payload.getErrorTypes())
    }

    /**
     * An errortypes set contains every type included in the default filename
     */
    @Test
    fun verifyFileNameMixedTypes() {
        val config = convertToImmutableConfig(BugsnagTestUtils.generateConfiguration())
        val file =
            File("1504255147933_0000111122223333aaaabbbbcccc9999_,android,c,reactnativejs,dart_my-uuid-123_.json")
        val payload = EventPayload(config.apiKey, null, file, Notifier(), config)
        assertEquals(ErrorType.values().toSet(), payload.getErrorTypes())
    }
}
