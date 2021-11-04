package com.bugsnag.android

import com.bugsnag.android.SeverityReason.REASON_CALLBACK_SPECIFIED
import com.bugsnag.android.SeverityReason.REASON_HANDLED_ERROR
import com.bugsnag.android.SeverityReason.REASON_HANDLED_EXCEPTION
import com.bugsnag.android.SeverityReason.REASON_LOG
import com.bugsnag.android.SeverityReason.REASON_PROMISE_REJECTION
import com.bugsnag.android.SeverityReason.REASON_STRICT_MODE
import com.bugsnag.android.SeverityReason.REASON_UNHANDLED_EXCEPTION
import com.bugsnag.android.SeverityReason.REASON_USER_SPECIFIED
import com.bugsnag.android.SeverityReason.newInstance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SeverityReasonTest {

    @Test
    fun testHandled() {
        val handled = newInstance(REASON_HANDLED_EXCEPTION)
        assertNotNull(handled)
        assertFalse(handled.unhandled)
        assertEquals(Severity.WARNING, handled.currentSeverity)
        assertFalse(handled.unhandledOverridden)
    }

    @Test
    fun testHandledError() {
        val handled = newInstance(REASON_HANDLED_ERROR)
        assertNotNull(handled)
        assertFalse(handled.unhandled)
        assertEquals(Severity.WARNING, handled.currentSeverity)
        assertFalse(handled.unhandledOverridden)
    }

    @Test
    fun testUnhandled() {
        val unhandled = newInstance(REASON_UNHANDLED_EXCEPTION)
        assertNotNull(unhandled)
        assertTrue(unhandled.unhandled)
        assertEquals(Severity.ERROR, unhandled.currentSeverity)
        assertFalse(unhandled.unhandledOverridden)
    }

    @Test
    fun testHandledOverride() {
        val handled = newInstance(REASON_HANDLED_EXCEPTION)
        assertNotNull(handled)
        assertFalse(handled.unhandled)
        handled.unhandled = true
        assertTrue(handled.unhandledOverridden)
    }

    @Test
    fun testUnhandledOverride() {
        val unhandled = newInstance(REASON_UNHANDLED_EXCEPTION)
        assertNotNull(unhandled)
        assertTrue(unhandled.unhandled)
        unhandled.unhandled = false
        assertEquals(Severity.ERROR, unhandled.currentSeverity)
        assertTrue(unhandled.unhandledOverridden)
    }

    @Test
    fun testUserSpecified() {
        val userSpecified = newInstance(REASON_USER_SPECIFIED, Severity.INFO, null)
        assertNotNull(userSpecified)
        assertFalse(userSpecified.unhandled)
        assertEquals(Severity.INFO, userSpecified.currentSeverity)
    }

    @Test
    fun testStrictMode() {
        val strictMode = newInstance(REASON_STRICT_MODE, null, "Test")
        assertNotNull(strictMode)
        assertTrue(strictMode.unhandled)
        assertEquals(Severity.WARNING, strictMode.currentSeverity)
        assertEquals("Test", strictMode.attributeValue)
    }

    @Test
    fun testPromiseRejection() { // invoked via react native
        val unhandled = newInstance(REASON_PROMISE_REJECTION)
        assertNotNull(unhandled)
        assertTrue(unhandled.unhandled)
        assertEquals(Severity.ERROR, unhandled.currentSeverity)
    }

    @Test
    fun testLog() { // invoked via Unity
        val unhandled = newInstance(REASON_LOG, Severity.WARNING, null)
        assertNotNull(unhandled)
        assertFalse(unhandled.unhandled)
        assertEquals(Severity.WARNING, unhandled.currentSeverity)
    }

    @Test
    fun testCallbackSpecified() {
        val handled = newInstance(REASON_HANDLED_EXCEPTION)
        assertEquals(REASON_HANDLED_EXCEPTION, handled.calculateSeverityReasonType())
        handled.currentSeverity = Severity.INFO
        assertEquals(REASON_CALLBACK_SPECIFIED, handled.calculateSeverityReasonType())
    }

    @Test
    fun testInvalidUserSpecified() {
        val handled = newInstance(REASON_CALLBACK_SPECIFIED)
        assertEquals(REASON_CALLBACK_SPECIFIED, handled.calculateSeverityReasonType())
        handled.currentSeverity = Severity.INFO
        assertEquals(REASON_CALLBACK_SPECIFIED, handled.calculateSeverityReasonType())
    }

    @Test(expected = IllegalArgumentException::class)
    fun testInvalidStrictmodeVal() {
        newInstance(REASON_STRICT_MODE)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testInvalidHandledVal() {
        newInstance(REASON_HANDLED_EXCEPTION, Severity.ERROR, "Whoops")
    }
}
