package com.bugsnag.android;

import org.junit.Test;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertNull;

public class HandledStateTest {

    @Test
    public void testHandledEventState() throws Exception {
        HandledState state = new HandledState(Severity.WARNING, false);
        assertNotNull(state);
        assertNull(state.getSeverityReasonType());
        assertFalse(state.isUnhandled());
        assertTrue(state.isDefaultSeverity(Severity.WARNING));
        assertFalse(state.isDefaultSeverity(Severity.INFO));
    }

    @Test
    public void testUnhandledEventState() throws Exception {
        HandledState state = new HandledState(Severity.ERROR, true);
        assertNotNull(state);
        assertNotNull(state.getSeverityReasonType());
        assertTrue(state.isUnhandled());
        assertTrue(state.isDefaultSeverity(Severity.ERROR));
        assertFalse(state.isDefaultSeverity(Severity.WARNING));
    }

}
