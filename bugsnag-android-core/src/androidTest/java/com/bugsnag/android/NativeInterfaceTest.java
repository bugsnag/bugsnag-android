package com.bugsnag.android;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import androidx.test.core.app.ApplicationProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.nio.file.Files;
import java.util.Map;

public class NativeInterfaceTest {

    private Client client;

    @Before
    public void setUp() throws Exception {
        client = BugsnagTestUtils.generateClient();
    }

    @After
    public void tearDown() {
        client.close();
    }

    @Test
    public void getMetaData() {
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

    @Test
    public void getPersistenceDir() {
        Client client = BugsnagTestUtils.generateClient();
        NativeInterface.setClient(client);
        File cacheDir = ApplicationProvider.getApplicationContext().getCacheDir();
        String observed = NativeInterface.getNativeReportPath();
        assertEquals(cacheDir.getAbsolutePath() + "/bugsnag-native", observed);
    }
}
