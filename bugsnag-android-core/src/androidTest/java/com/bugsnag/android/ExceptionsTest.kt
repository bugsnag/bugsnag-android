package com.bugsnag.android

import com.bugsnag.android.BugsnagTestUtils.generateImmutableConfig
import com.bugsnag.android.BugsnagTestUtils.streamableToJsonArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

import java.lang.Thread

class ExceptionsTest {

    private lateinit var config: ImmutableConfig

    @Before
    fun setUp() {
        config = generateImmutableConfig()
    }

    @Test
    fun testBasicException() {
        val oops = RuntimeException("oops")
        val exceptions = Exceptions(config, BugsnagException(oops))
        val exceptionsJson = streamableToJsonArray(exceptions)

        assertEquals(1, exceptionsJson.length().toLong())

        val firstException = exceptionsJson.get(0) as JSONObject
        assertEquals("java.lang.RuntimeException", firstException.get("errorClass"))
        assertEquals("oops", firstException.get("message"))
        assertNotNull(firstException.get("stacktrace"))
    }

    @Test
    fun testCauseException() {
        val ex = RuntimeException("oops", Exception("cause"))
        val exceptions = Exceptions(config, BugsnagException(ex))
        val exceptionsJson = streamableToJsonArray(exceptions)

        assertEquals(2, exceptionsJson.length().toLong())

        val firstException = exceptionsJson.get(0) as JSONObject
        assertEquals("java.lang.RuntimeException", firstException.get("errorClass"))
        assertEquals("oops", firstException.get("message"))
        assertNotNull(firstException.get("stacktrace"))

        val causeException = exceptionsJson.get(1) as JSONObject
        assertEquals("java.lang.Exception", causeException.get("errorClass"))
        assertEquals("cause", causeException.get("message"))
        assertNotNull(causeException.get("stacktrace"))
    }

    @Test
    fun testNamedException() {
        val element = StackTraceElement("Class", "method", "Class.java", 123)
        val frames = arrayOf(element)
        val event = Event.Builder(
            config, "RuntimeException",
            "Example message", frames, BugsnagTestUtils.generateSessionTracker(),
            Thread.currentThread(), MetaData()
        ).build()
        val exceptions = Exceptions(config, BugsnagException(event.exception))

        val json = streamableToJsonArray(exceptions)
        val exceptionJson = json.getJSONObject(0)
        assertEquals("RuntimeException", exceptionJson.get("errorClass"))
        assertEquals("Example message", exceptionJson.get("message"))

        val stackframeJson = exceptionJson.getJSONArray("stacktrace").getJSONObject(0)
        assertEquals("Class.method", stackframeJson.get("method"))
        assertEquals("Class.java", stackframeJson.get("file"))
        assertEquals(123, stackframeJson.get("lineNumber"))
    }
}
