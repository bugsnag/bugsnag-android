package com.bugsnag.android;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import androidx.annotation.NonNull;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.SmallTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

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
            @Override
            public void deliver(@NonNull SessionTrackingPayload payload,
                                @NonNull Configuration config)
                throws DeliveryFailureException {}

            @Override
            public void deliver(@NonNull Report report,
                                @NonNull Configuration config)
                throws DeliveryFailureException {
                lastReport = report;
            }
        });
        client = new Client(ApplicationProvider.getApplicationContext(), config);
    }

    @After
    public void tearDown() throws Exception {
        lastReport = null;
        client.close();
    }

    @Test
    public void testCallbackOrderPreserved() {
        config.beforeSend(new BeforeSend() {
            @Override
            public boolean run(@NonNull Report report) {
                result = result + "a";
                return true;
            }
        });
        config.beforeSend(new BeforeSend() {
            @Override
            public boolean run(@NonNull Report report) {
                result = result + "b";
                return true;
            }
        });
        config.beforeSend(new BeforeSend() {
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
        config.beforeSend(new BeforeSend() {
            @Override
            public boolean run(@NonNull Report report) {
                result = result + "a";
                return true;
            }
        });
        config.beforeSend(new BeforeSend() {
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
        config.beforeSend(new BeforeSend() {
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
