package com.bugsnag.android

import com.bugsnag.android.BugsnagTestUtils.generateImmutableConfig
import com.bugsnag.android.BugsnagTestUtils.streamableToJsonArray
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BugsnagExceptionTest {

    private val config = generateImmutableConfig()

    @Test
    fun stacktraceConstructorSerialisation() {
        val trace = arrayOf(StackTraceElement("Foo", "bar", "Foo.kt", 1))
        val exc = jsonObjectFromException(BugsnagException("MyClass", "Custom message", trace))
        assertEquals("MyClass", exc.get("errorClass"))
        assertEquals("Custom message", exc.get("message"))
        assertEquals("android", exc.get("type"))

        val stacktrace = exc.get("stacktrace") as JSONArray
        assertTrue(stacktrace.length() > 0)
    }

    @Test
    fun throwableConstructorSerialisation() {
        val exc = jsonObjectFromException(BugsnagException(RuntimeException("oops")))
        assertEquals("java.lang.RuntimeException", exc.get("errorClass"))
        assertEquals("oops", exc.get("message"))
        assertEquals("android", exc.get("type"))

        val stacktrace = exc.get("stacktrace") as JSONArray
        assertTrue(stacktrace.length() > 0)
    }

    @Test
    fun overrideName() {
        val bugsnagException = BugsnagException(RuntimeException("oops"))
        bugsnagException.name = "FatalNetworkError"
        val exc = jsonObjectFromException(bugsnagException)
        assertEquals("FatalNetworkError", exc.get("errorClass"))
    }

    @Test
    fun overrideMessage() {
        val bugsnagException = BugsnagException(RuntimeException("oops"))
        bugsnagException.setMessage("User not found")
        val exc = jsonObjectFromException(bugsnagException)
        assertEquals("User not found", exc.get("message"))
    }

    @Test
    fun overrideType() {
        val bugsnagException = BugsnagException(RuntimeException("oops"))
        bugsnagException.type = "browserjs"
        val exc = jsonObjectFromException(bugsnagException)
        assertEquals("browserjs", exc.get("type"))
    }

    @Test
    fun nestedBugsnagException() {
        val nestedThrowable = BugsnagException(BugsnagException(RuntimeException("oops")))
        val exc = jsonObjectFromException(nestedThrowable)
        assertEquals("java.lang.RuntimeException", exc.get("errorClass"))
        assertEquals("oops", exc.get("message"))
        assertEquals("android", exc.get("type"))

        val stacktrace = exc.get("stacktrace") as JSONArray
        assertTrue(stacktrace.length() > 0)
    }

    private fun jsonObjectFromException(bugsnagException: BugsnagException): JSONObject {
        val exceptions = Exceptions(config, bugsnagException)
        val json = streamableToJsonArray(exceptions)
        val exc = json.get(0) as JSONObject
        return exc
    }
}
