package com.bugsnag.android;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import android.support.annotation.NonNull;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class BeforeSendTest {

    private Client client;
    private Configuration config;
    private Report lastReport;
    private String result = "";

    /**
     * Configures a client
     */
    @Before
    public void setUp() throws Exception {
        result = "";
        config = new Configuration("key");
        config.setDelivery(new Delivery() {
            @NotNull
            @Override
            public DeliveryStatus deliver(@NotNull Report report,
                                          @NotNull DeliveryParams deliveryParams) {
                lastReport = report;
                return DeliveryStatus.DELIVERED;
            }

            @NotNull
            @Override
            public DeliveryStatus deliver(@NotNull SessionTrackingPayload payload,
                                          @NotNull DeliveryParams deliveryParams) {
                return DeliveryStatus.DELIVERED;
            }
        });
        client = new Client(InstrumentationRegistry.getContext(), config);
    }

    @After
    public void tearDown() throws Exception {
        lastReport = null;
        client.close();
    }

    @Test
    public void testCallbackOrderPreserved() {
        config.addBeforeSend(new BeforeSend() {
            @Override
            public boolean run(@NonNull Report report) {
                result = result + "a";
                return true;
            }
        });
        config.addBeforeSend(new BeforeSend() {
            @Override
            public boolean run(@NonNull Report report) {
                result = result + "b";
                return true;
            }
        });
        config.addBeforeSend(new BeforeSend() {
            @Override
            public boolean run(@NonNull Report report) {
                result = result + "c";
                return true;
            }
        });
        client.notifyBlocking(new Exception("womp womp"));
        assertEquals("abc", result);
        assertNotNull(lastReport);
    }

    @Test
    public void testCancelReport() {
        config.addBeforeSend(new BeforeSend() {
            @Override
            public boolean run(@NonNull Report report) {
                result = result + "a";
                return true;
            }
        });
        config.addBeforeSend(new BeforeSend() {
            @Override
            public boolean run(@NonNull Report report) {
                result = result + "b";
                return false;
            }
        });
        client.notifyBlocking(new Exception("womp womp"));
        assertEquals("ab", result);
        assertNull(lastReport);
    }

    @Test
    public void testAlterReport() {
        config.addBeforeSend(new BeforeSend() {
            @Override
            public boolean run(@NonNull Report report) {
                report.getError().setGroupingHash("123");
                return true;
            }
        });
        client.notifyBlocking(new Exception("womp womp"));
        assertEquals("123", lastReport.getError().getGroupingHash());
    }
}
