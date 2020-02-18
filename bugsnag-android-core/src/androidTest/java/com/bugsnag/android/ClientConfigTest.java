package com.bugsnag.android;

import static com.bugsnag.android.BugsnagTestUtils.generateConfiguration;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.SmallTest;

import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

@SmallTest
public class ClientConfigTest {

    private Configuration config;
    private Client client;

    /**
     * Generates a configuration and clears sharedPrefs values to begin the test with a clean slate
     */
    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        config = generateConfiguration();
        client = new Client(context, config);
    }

    @After
    public void tearDown() {
        client.close();
    }

    @Test
    public void testSetContext() {
        client.setContext("JunitTest");
        assertEquals("JunitTest", client.getContext());
        assertNull(config.getContext());
    }

    @Test
    public void testCustomDeliveryOverride() {
        Context context = ApplicationProvider.getApplicationContext();
        config = generateConfiguration();
        Delivery customDelivery = new Delivery() {
            @NotNull
            @Override
            public DeliveryStatus deliver(@NotNull EventPayload payload,
                                          @NotNull DeliveryParams deliveryParams) {
                return DeliveryStatus.DELIVERED;
            }

            @NotNull
            @Override
            public DeliveryStatus deliver(@NotNull Session payload,
                                          @NotNull DeliveryParams deliveryParams) {
                return DeliveryStatus.DELIVERED;
            }
        };
        config.setDelivery(customDelivery);
        client = new Client(context, config);
        assertEquals(customDelivery, client.getConfig().getDelivery());
    }
}
