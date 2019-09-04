package com.bugsnag.android;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;

import org.junit.Test;

import java.util.Map;

public class NativeInterfaceTest {

    @Test
    public void getMetaData() {
        Client client = BugsnagTestUtils.generateClient();
        NativeInterface.setClient(client);
        assertNotSame(client.config.getMetaData().store, NativeInterface.getMetaData());
    }

    @Test
    public void testClearTabClearsTheTab() {
        // Creates a client object with the test context and no-op delivery
        Client client = BugsnagTestUtils.generateClient();
        NativeInterface.setClient(client);

        client.addToTab("foo", "bar", "baz");
        Map<String, Object> tab = client.getMetaData().getTab("foo");
        assertEquals(1, tab.size());
        assertEquals("baz", tab.get("bar"));

        NativeInterface.clearTab("foo");
        tab = client.getMetaData().getTab("foo");
        assertEquals(0, tab.size());
    }
}
