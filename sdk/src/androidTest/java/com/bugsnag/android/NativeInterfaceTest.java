package com.bugsnag.android;

import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

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
    public void addToTab() {
        Client client = BugsnagTestUtils.generateClient();
        NativeInterface.setClient(client);
        NativeInterface.addToTab("app", "buildno", "0.1");
        NativeInterface.addToTab("app", "args", "-print 1");
        NativeInterface.addToTab("info", "cache", false);

        Map<String, Object> metadata = NativeInterface.getMetaData();
        @SuppressWarnings("unchecked")
        Map<String, Object> app = (Map<String, Object>)metadata.get("app");
        assertSame("0.1", app.get("buildno"));
        assertSame("-print 1", app.get("args"));
        @SuppressWarnings("unchecked")
        Map<String, Object> info = (Map<String, Object>)metadata.get("info");
        assertSame(false, info.get("cache"));
    }

    @Test
    public void clearTab() {
        Client client = BugsnagTestUtils.generateClient();
        NativeInterface.setClient(client);
        NativeInterface.addToTab("app", "buildno", "0.1");
        NativeInterface.addToTab("app", "args", "-print 1");
        NativeInterface.addToTab("info", "cache", false);
        NativeInterface.clearTab("info");

        Map<String, Object> metadata = NativeInterface.getMetaData();
        @SuppressWarnings("unchecked")
        Map<String, Object> app = (Map<String, Object>)metadata.get("app");
        assertSame("0.1", app.get("buildno"));
        assertSame("-print 1", app.get("args"));
        assertNull(metadata.get("info"));
    }
}
