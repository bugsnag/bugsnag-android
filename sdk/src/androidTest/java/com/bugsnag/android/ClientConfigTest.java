package com.bugsnag.android;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class ClientConfigTest {

    private Configuration config;
    private Client client;

    /**
     * Generates a configuration and clears sharedPrefs values to begin the test with a clean slate
     * @throws Exception if initialisation failed
     */
    @Before
    public void setUp() throws Exception {
        Context context = InstrumentationRegistry.getContext();
        config = new Configuration("api-key");
        client = new Client(context, config);
    }

    @Test
    public void testSetReleaseStage() throws Exception {
        client.setReleaseStage("beta");
        assertEquals("beta", config.getReleaseStage());
    }

    @Test
    public void testSetAutoCaptureSessions() throws Exception {
        client.setAutoCaptureSessions(true);
        assertEquals(true, config.shouldAutoCaptureSessions());
    }

    @Test
    public void testSetAppVersion() throws Exception {
        client.setAppVersion("5.6.7");
        assertEquals("5.6.7", config.getAppVersion());
    }

    @Test
    public void testSetContext() throws Exception {
        client.setContext("JunitTest");
        assertEquals("JunitTest", client.getContext());
        assertEquals("JunitTest", config.getContext());
    }

    @Test
    public void testSetEndpoint() throws Exception {
        client.setEndpoint("http://example.com/bugsnag");
        assertEquals("http://example.com/bugsnag", config.getEndpoint());
    }

    @Test
    public void testSetBuildUuid() throws Exception {
        client.setBuildUUID("gh905");
        assertEquals("gh905", config.getBuildUUID());
    }

    @Test
    public void testSetIgnoreClasses() throws Exception {
        client.setIgnoreClasses("RuntimeException", "Foo");
        assertArrayEquals(new String[]{"RuntimeException", "Foo"}, config.getIgnoreClasses());
    }

    @Test
    public void testSetNotifyReleaseStages() throws Exception {
        client.setNotifyReleaseStages("beta", "prod");
        assertArrayEquals(new String[]{"beta", "prod"}, config.getNotifyReleaseStages());
    }

    @Test
    public void testSetSendThreads() throws Exception {
        client.setSendThreads(false);
        assertFalse(config.getSendThreads());
    }

}
