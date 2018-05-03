package com.bugsnag.android;


import org.junit.Before;
import org.junit.Test;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static com.bugsnag.android.DeliveryFailureException.Reason.CONNECTIVITY;
import static com.bugsnag.android.DeliveryFailureException.Reason.REQUEST_FAILURE;
import static junit.framework.Assert.*;

public class DeliveryCompatTest {

    private final Configuration config = BugsnagTestUtils.generateConfiguration();
    private DeliveryCompat deliveryCompat;

    private AtomicInteger defaultCount;
    private AtomicInteger customCount;

    @Before
    public void setUp() throws Exception {
        defaultCount = new AtomicInteger();
        customCount = new AtomicInteger();

        Delivery baseDelivery = new Delivery() {
            @Override
            public void deliver(SessionTrackingPayload payload,
                                Configuration config) throws DeliveryFailureException {
                defaultCount.incrementAndGet();
            }

            @Override
            public void deliver(Report report,
                                Configuration config) throws DeliveryFailureException {
                defaultCount.incrementAndGet();
            }
        };
        deliveryCompat = new DeliveryCompat(baseDelivery);

    }

    @Test
    public void deliverReport() throws Exception {
        Report report = null;
        deliveryCompat.deliver(report, config);
        assertEquals(1, defaultCount.get());

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
        assertEquals(1, defaultCount.get());
        assertEquals(1, customCount.get());
    }

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
        assertEquals(1, defaultCount.get());
        assertEquals(1, customCount.get());
    }

    @Test
    public void testClientCompat() {
        Client client = BugsnagTestUtils.generateClient();
        Delivery delivery = client.config.getDelivery();
        assertTrue(delivery instanceof DeliveryCompat);
        DeliveryCompat compat = (DeliveryCompat) delivery;

        assertNull(compat.errorReportApiClient);
        assertNull(compat.sessionTrackingApiClient);

        ErrorReportApiClient errorClient = BugsnagTestUtils.generateErrorReportApiClient();
        client.setErrorReportApiClient(errorClient);

        assertEquals(errorClient, compat.errorReportApiClient);
        assertNull(compat.sessionTrackingApiClient);

        SessionTrackingApiClient sessionClient = BugsnagTestUtils.generateSessionTrackingApiClient();
        client.setSessionTrackingApiClient(sessionClient);

        assertEquals(errorClient, compat.errorReportApiClient);
        assertEquals(sessionClient, compat.sessionTrackingApiClient);
    }

    @Test
    public void testExceptionConversion() {
        BadResponseException requestFailExc = new BadResponseException("test", 400);
        DeliveryFailureException responseFail = deliveryCompat.convertException(requestFailExc);
        assertEquals(REQUEST_FAILURE ,responseFail.reason);

        NetworkException connectivityExc = new NetworkException("test", new RuntimeException(""));
        DeliveryFailureException connectivityFail = deliveryCompat.convertException(connectivityExc);
        assertEquals(CONNECTIVITY ,connectivityFail.reason);

        assertNull(deliveryCompat.convertException(new RuntimeException()));
    }

}
