package com.bugsnag.android;

import static org.junit.Assert.assertEquals;

import android.support.annotation.NonNull;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class ClientNotifyTest {

    private Client client;
    private FakeClient apiClient;

    /**
     * Generates a configuration and clears sharedPrefs values to begin the test with a clean slate
     *
     * @throws Exception if initialisation failed
     */
    @Before
    public void setUp() throws Exception {
        Configuration config = BugsnagTestUtils.generateConfiguration();
        apiClient = new FakeClient();
        config.setDelivery(apiClient);
        client = new Client(InstrumentationRegistry.getContext(), config);
    }

    @After
    public void tearDown() {
        client.close();
    }

    @Test
    public void testNotifyBlockingDefaultSeverity() {
        client.notifyBlocking(new RuntimeException("Testing"));
        assertEquals(Severity.WARNING, apiClient.report.getError().getSeverity());
    }

    @Test
    public void testNotifyBlockingCallback() {
        client.notifyBlocking(new RuntimeException("Testing"), new Callback() {
            @Override
            public void beforeNotify(@NonNull Report report) {
                report.getError().setUserName("Foo");
            }
        });
        Error error = apiClient.report.getError();
        assertEquals(Severity.WARNING, error.getSeverity());
        assertEquals("Foo", error.getUser().getName());
    }

    @Test
    public void testNotifyBlockingCustomSeverity() {
        client.notifyBlocking(new RuntimeException("Testing"), Severity.INFO);
        assertEquals(Severity.INFO, apiClient.report.getError().getSeverity());
    }

    @Test
    public void testNotifyBlockingCustomStackTrace() {
        StackTraceElement[] stacktrace = {
            new StackTraceElement("MyClass", "MyMethod", "MyFile", 5)
        };

        client.notifyBlocking("Name", "Message", stacktrace, new Callback() {
            @Override
            public void beforeNotify(@NonNull Report report) {
                report.getError().setSeverity(Severity.ERROR);
            }
        });
        Error error = apiClient.report.getError();
        assertEquals(Severity.ERROR, error.getSeverity());
        assertEquals("Name", error.getExceptionName());
        assertEquals("Message", error.getExceptionMessage());
    }

    static class FakeClient implements Delivery {

        CountDownLatch latch = new CountDownLatch(1);
        Report report;

        @Override
        public void deliver(@NonNull Report report,
                            @NonNull Configuration config) throws DeliveryFailureException {
            this.report = report;
            latch.countDown();
        }

        @Override
        public void deliver(@NonNull SessionTrackingPayload payload,
                            @NonNull Configuration config) throws DeliveryFailureException {
        }
    }

}
