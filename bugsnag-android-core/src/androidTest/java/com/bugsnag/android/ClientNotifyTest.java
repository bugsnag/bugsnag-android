package com.bugsnag.android;

import static org.junit.Assert.assertEquals;

import androidx.annotation.NonNull;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.SmallTest;

import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;

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
        client = new Client(ApplicationProvider.getApplicationContext(), config);
    }

    @After
    public void tearDown() {
        client.close();
    }

    @Test
    public void testNotifyBlockingDefaultSeverity() {
        client.notifyBlocking(new RuntimeException("Testing"));
        assertEquals(Severity.WARNING, apiClient.report.getEvent().getSeverity());
    }

    @Test
    public void testNotifyBlockingCallback() {
        client.notifyBlocking(new RuntimeException("Testing"), new OnError() {
            @Override
            public boolean run(@NonNull Event event) {
                event.setUserName("Foo");
                return true;
            }
        });
        Event event = apiClient.report.getEvent();
        assertEquals(Severity.WARNING, event.getSeverity());
        assertEquals("Foo", event.getUser().getName());
    }

    @Test
    public void testNotifyBlockingCustomSeverity() {
        client.notifyBlocking(new RuntimeException("Testing"), Severity.INFO);
        assertEquals(Severity.INFO, apiClient.report.getEvent().getSeverity());
    }

    @Test
    public void testNotifyBlockingCustomStackTrace() {
        StackTraceElement[] stacktrace = {
            new StackTraceElement("MyClass", "MyMethod", "MyFile", 5)
        };

        client.notifyBlocking("Name", "Message", stacktrace, new OnError() {
            @Override
            public boolean run(@NonNull Event event) {
                event.setSeverity(Severity.ERROR);
                return true;
            }
        });
        Event event = apiClient.report.getEvent();
        assertEquals(Severity.ERROR, event.getSeverity());
        assertEquals("Name", event.getExceptionName());
        assertEquals("Message", event.getExceptionMessage());
    }

    static class FakeClient implements Delivery {

        CountDownLatch latch = new CountDownLatch(1);
        Report report;

        @NotNull
        @Override
        public DeliveryStatus deliver(@NotNull SessionPayload payload,
                                      @NotNull DeliveryParams deliveryParams) {
            return DeliveryStatus.DELIVERED;
        }

        @NotNull
        @Override
        public DeliveryStatus deliver(@NotNull Report report,
                                      @NotNull DeliveryParams deliveryParams) {
            this.report = report;
            latch.countDown();
            return DeliveryStatus.DELIVERED;
        }
    }

}
