package com.bugsnag.android;

import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;

public class NativeInterfaceTest {

    private Client client;

    @Before
    public void setUp() {
        client = BugsnagTestUtils.generateClient();
    }

    @After
    public void tearDown() {
        client.close();
    }

    @Test
    public void getMetadata() {
        NativeInterface.setClient(client);
        assertNotSame(client.metadataState.getMetadata().toMap(), NativeInterface.getMetadata());
    }

    @Test
    public void addToTab() {
        Client client = BugsnagTestUtils.generateClient();
        NativeInterface.setClient(client);
        NativeInterface.addMetadata("app", "buildno", "0.1");
        NativeInterface.addMetadata("app", "args", "-print 1");
        NativeInterface.addMetadata("info", "cache", false);

        Map<String, Object> metadata = NativeInterface.getMetadata();
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
        NativeInterface.addMetadata("app", "buildno", "0.1");
        NativeInterface.addMetadata("app", "args", "-print 1");
        NativeInterface.addMetadata("info", "cache", false);
        NativeInterface.clearMetadata("info", null);

        Map<String, Object> metadata = NativeInterface.getMetadata();
        @SuppressWarnings("unchecked")
        Map<String, Object> app = (Map<String, Object>)metadata.get("app");
        assertSame("0.1", app.get("buildno"));
        assertSame("-print 1", app.get("args"));
        assertNull(metadata.get("info"));
    }
}
