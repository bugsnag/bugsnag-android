package com.bugsnag.android

import android.content.Context
import android.os.Build
import android.os.strictmode.FakeStrictModeViolation
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.SdkSuppress
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class StrictModeViolationTest {

    private lateinit var context: Context
    private lateinit var config: Configuration
    private lateinit var client: Client

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        config = BugsnagTestUtils.generateConfiguration()
        client = Client(context, config)
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.P)
    fun testThreadViolationCapture() {
        val listener = BugsnagThreadViolationListener(client)
        var event: Event? = null
        client.addOnError(
            OnErrorCallback {
                event = it
                true
            }
        )

        // trigger a violation and assert it is converted to an event
        listener.onThreadViolation(FakeStrictModeViolation())
        val payload = checkNotNull(event)
        assertEquals(Severity.INFO, payload.severity)

        // validate error object
        val err = payload.errors.single()
        assertEquals("StrictMode policy violation detected: ThreadPolicy", err.errorMessage)
        assertEquals("android.os.strictmode.FakeStrictModeViolation", err.errorClass)
        assertEquals(ErrorType.ANDROID, err.type)

        // validate first stackframe
        val frame = err.stacktrace.first()
        assertEquals(
            "com.bugsnag.android.StrictModeViolationTest.testThreadViolationCapture",
            frame.method
        )
        assertEquals("StrictModeViolationTest.kt", frame.file)
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.P)
    fun testVmViolationCapture() {
        val listener = BugsnagVmViolationListener(client)
        var event: Event? = null
        client.addOnError(
            OnErrorCallback {
                event = it
                true
            }
        )

        // trigger a violation and assert it is converted to an event
        listener.onVmViolation(FakeStrictModeViolation())
        val payload = checkNotNull(event)
        assertEquals(Severity.INFO, payload.severity)

        // validate error object
        val err = payload.errors.single()
        assertEquals("StrictMode policy violation detected: VmPolicy", err.errorMessage)
        assertEquals("android.os.strictmode.FakeStrictModeViolation", err.errorClass)
        assertEquals(ErrorType.ANDROID, err.type)

        // validate first stackframe
        val frame = err.stacktrace.first()
        assertEquals(
            "com.bugsnag.android.StrictModeViolationTest.testVmViolationCapture",
            frame.method
        )
        assertEquals("StrictModeViolationTest.kt", frame.file)
    }
}
