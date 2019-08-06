package com.bugsnag.android;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.SmallTest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

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
        Context context = ApplicationProvider.getApplicationContext();
        config = new Configuration("api-key");
        client = new Client(context, config);
    }

    @After
    public void tearDown() {
        client.close();
    }

    @Test
    public void testSetReleaseStage() throws Exception {
        client.setReleaseStage("beta");
        assertEquals("beta", config.getReleaseStage());
    }

    @Test
    public void testSetAutoCaptureSessions() throws Exception {
        client.setAutoCaptureSessions(true);
        assertEquals(true, config.getAutoCaptureSessions());
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

    @SuppressWarnings("deprecation")
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

    @Test
    public void testDefaultClientDelivery() {
        assertFalse(client.config.getDelivery() instanceof DeliveryCompat);
    }

    @Test
    public void testCustomDeliveryOverride() {
        Context context = ApplicationProvider.getApplicationContext();
        config = BugsnagTestUtils.generateConfiguration();
        Delivery customDelivery = new Delivery() {
            @Override
            public void deliver(@NonNull SessionTrackingPayload payload,
                                @NonNull Configuration config)
                throws DeliveryFailureException {}

            @Override
            public void deliver(@NonNull Report report,
                                @NonNull Configuration config)
                throws DeliveryFailureException {}

        };
        config.setDelivery(customDelivery);
        client = new Client(context, config);
        assertEquals(customDelivery, client.config.getDelivery());
    }
}
