package com.bugsnag.android;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Before;
import org.junit.Test;

public class NotifierTest {

    private Notifier notifier;

    @Before
    public void setUp() throws Exception {
        notifier = new Notifier();
    }

    @Test
    public void testName() {
        assertEquals("Android Bugsnag Notifier", notifier.getName());
        String expected = "CrossPlatformFramework";
        notifier.setName(expected);
        assertEquals(expected, notifier.getName());
    }

    @Test
    public void testVersion() {
        assertNotNull(notifier.getVersion());
        String expected = "1.2.3";
        notifier.setVersion(expected);
        assertEquals(expected, notifier.getVersion());
    }

    @Test
    public void testUrl() {
        assertEquals("https://bugsnag.com", notifier.getURL());
        String expected = "http://example.com";
        notifier.setURL(expected);
        assertEquals(expected, notifier.getURL());
    }
}
