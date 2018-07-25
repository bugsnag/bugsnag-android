package com.bugsnag.android;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class DeliveryCompatTest {

    private final Configuration config = BugsnagTestUtils.generateConfiguration();
    private DeliveryCompat deliveryCompat;

    private AtomicInteger customCount;

    /**
     * Generates a Delivery instance that increments a counter on each request
     *
     * @throws Exception if setup failed
     */
    @Before
    public void setUp() throws Exception {
        customCount = new AtomicInteger();
        deliveryCompat = new DeliveryCompat();
    }

    @After
    public void tearDown() throws Exception {
        Async.cancelTasks();
    }

    @SuppressWarnings("deprecation")
    @Test
    public void deliverReport() throws Exception {
        Report report = null;
        deliveryCompat.deliver(report, config);

        deliveryCompat.errorReportApiClient = new ErrorReportApiClient() {
            @Override
            public void postReport(String urlString,
                                   Report report,
                                   Map<String, String> headers)
                throws NetworkException, BadResponseException {
                customCount.incrementAndGet();
            }
        };

        deliveryCompat.deliver(report, config);
        assertEquals(1, customCount.get());
    }

    @SuppressWarnings("deprecation")
    @Test
    public void deliverSession() throws Exception {
        SessionTrackingPayload payload = null;
        deliveryCompat.deliver(payload, config);

        deliveryCompat.sessionTrackingApiClient = new SessionTrackingApiClient() {
            @Override
            public void postSessionTrackingPayload(String urlString,
                                                   SessionTrackingPayload payload,
                                                   Map<String, String> headers)
                throws NetworkException, BadResponseException {
                customCount.incrementAndGet();
            }
        };

        deliveryCompat.deliver(payload, config);
        assertEquals(1, customCount.get());
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testClientCompat() {
        Client client = BugsnagTestUtils.generateClient();
        Delivery delivery = client.config.getDelivery();
        assertFalse(delivery instanceof DeliveryCompat);

        ErrorReportApiClient errorReportApiClient = BugsnagTestUtils.generateErrorReportApiClient();
        client.setErrorReportApiClient(errorReportApiClient);

        assertTrue(client.config.getDelivery() instanceof DeliveryCompat);

        DeliveryCompat compat = (DeliveryCompat) client.config.getDelivery();
        assertEquals(errorReportApiClient, compat.errorReportApiClient);
        assertNull(compat.sessionTrackingApiClient);

        SessionTrackingApiClient sessionClient
            = BugsnagTestUtils.generateSessionTrackingApiClient();
        client.setSessionTrackingApiClient(sessionClient);

        assertEquals(errorReportApiClient, compat.errorReportApiClient);
        assertEquals(sessionClient, compat.sessionTrackingApiClient);
    }

    @SuppressWarnings("deprecation")
    @Test(expected = DeliveryFailureException.class)
    public void testExceptionConversion() throws Exception {
        deliveryCompat.handleException(new NetworkException("", null));
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testSwallowExceptionConversion() throws Exception { // no exception thrown
        deliveryCompat.handleException(new BadResponseException("", 0));
    }
}
