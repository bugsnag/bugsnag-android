package com.bugsnag.android;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

/**
 * Ensures that Bugsnag's API surface is able to deal with any unexpected null input that might
 * cause the SDK to crash.
 */
public class StrictModeFacadeTest {

    @Test
    public void testInvalidThreadListener() {
        BugsnagThreadViolationListener listener = new BugsnagThreadViolationListener(null);
        listener.onThreadViolation(null);
        assertNotNull(listener);
    }

    @Test
    public void testInvalidVmListener() {
        BugsnagVmViolationListener listener = new BugsnagVmViolationListener(null);
        listener.onVmViolation(null);
        assertNotNull(listener);
    }
}
