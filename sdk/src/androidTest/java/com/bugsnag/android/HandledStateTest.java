package com.bugsnag.android;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

public class HandledStateTest {

    @Test
    public void testHandled() throws Exception {
        HandledState handled = HandledState.newInstance(
            HandledState.REASON_HANDLED_EXCEPTION);
        assertNotNull(handled);
        assertFalse(handled.isUnhandled());
        assertEquals(Severity.WARNING, handled.getCurrentSeverity());
    }

    @Test
    public void testUnhandled() throws Exception {
        HandledState unhandled = HandledState.newInstance(
            HandledState.REASON_UNHANDLED_EXCEPTION);
        assertNotNull(unhandled);
        assertTrue(unhandled.isUnhandled());
        assertEquals(Severity.ERROR, unhandled.getCurrentSeverity());
    }

    @Test
    public void testUserSpecified() throws Exception {
        HandledState userSpecified = HandledState.newInstance(
            HandledState.REASON_USER_SPECIFIED, Severity.INFO, null);
        assertNotNull(userSpecified);
        assertFalse(userSpecified.isUnhandled());
        assertEquals(Severity.INFO, userSpecified.getCurrentSeverity());
    }

    @Test
    public void testStrictMode() throws Exception {
        HandledState strictMode = HandledState.newInstance(
            HandledState.REASON_STRICT_MODE, null, "Test");
        assertNotNull(strictMode);
        assertTrue(strictMode.isUnhandled());
        assertEquals(Severity.WARNING, strictMode.getCurrentSeverity());
        assertEquals("Test", strictMode.getAttributeValue());
    }

    @Test
    public void testPromiseRejection() throws Exception { // invoked via react native
        HandledState unhandled = HandledState.newInstance(
            HandledState.REASON_PROMISE_REJECTION);
        assertNotNull(unhandled);
        assertTrue(unhandled.isUnhandled());
        assertEquals(Severity.ERROR, unhandled.getCurrentSeverity());
    }

    @Test
    public void testLog() throws Exception { // invoked via Unity
        HandledState unhandled = HandledState.newInstance(HandledState.REASON_LOG, Severity.WARNING, null);
        assertNotNull(unhandled);
        assertFalse(unhandled.isUnhandled());
        assertEquals(Severity.WARNING, unhandled.getCurrentSeverity());
    }

    @Test
    public void testCallbackSpecified() throws Exception {
        HandledState handled = HandledState.newInstance(HandledState.REASON_HANDLED_EXCEPTION);
        assertEquals(HandledState.REASON_HANDLED_EXCEPTION,
            handled.calculateSeverityReasonType());

        handled.setCurrentSeverity(Severity.INFO);
        assertEquals(HandledState.REASON_CALLBACK_SPECIFIED,
            handled.calculateSeverityReasonType());
    }

    @Test
    public void testInvalidUserSpecified() throws Exception {
        HandledState handled = HandledState.newInstance(HandledState.REASON_CALLBACK_SPECIFIED);
        assertEquals(HandledState.REASON_CALLBACK_SPECIFIED,
            handled.calculateSeverityReasonType());

        handled.setCurrentSeverity(Severity.INFO);
        assertEquals(HandledState.REASON_CALLBACK_SPECIFIED,
            handled.calculateSeverityReasonType());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidStrictmodeVal() throws Exception {
        HandledState.newInstance(HandledState.REASON_STRICT_MODE);
    }

}
