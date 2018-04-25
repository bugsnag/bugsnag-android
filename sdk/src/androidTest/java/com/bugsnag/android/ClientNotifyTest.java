package com.bugsnag.android;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

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
        client = BugsnagTestUtils.generateClient();
        apiClient = new FakeClient();
        client.setErrorReportApiClient(apiClient);
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
            public void beforeNotify(Report report) {
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
            public void beforeNotify(Report report) {
                report.getError().setSeverity(Severity.ERROR);
            }
        });
        Error error = apiClient.report.getError();
        assertEquals(Severity.ERROR, error.getSeverity());
        assertEquals("Name", error.getExceptionName());
        assertEquals("Message", error.getExceptionMessage());
    }

    @Test
    public void testNotifyAsyncMetadata() throws Exception {
        MetaData metaData = new MetaData();
        metaData.addToTab("animals", "dog", true);

        client.notify(new RuntimeException("Foo"), metaData);
        apiClient.awaitReport();
        assertNotNull(apiClient.report);
        MetaData data = apiClient.report.getError().getMetaData();
        assertTrue((Boolean) data.getTab("animals").get("dog"));
    }

    @Test
    public void testNotifyAsyncSeverity() throws Exception {
        client.notify(new RuntimeException("Foo"), Severity.INFO);
        apiClient.awaitReport();
        assertNotNull(apiClient.report);
        assertEquals(Severity.INFO, apiClient.report.getError().getSeverity());
    }

    @Test
    public void testNotifyAsyncSeverityMetadata() throws Exception {
        MetaData metaData = new MetaData();
        metaData.addToTab("animals", "bird", "chicken");

        client.notify(new RuntimeException("Foo"), Severity.ERROR, metaData);
        apiClient.awaitReport();
        assertNotNull(apiClient.report);
        MetaData data = apiClient.report.getError().getMetaData();
        assertEquals("chicken", data.getTab("animals").get("bird"));
        assertEquals(Severity.ERROR, apiClient.report.getError().getSeverity());
    }

    @Test
    public void testNotifyAsyncCallback() throws Exception {
        client.notify(new RuntimeException("Foo"), new Callback() {
            @Override
            public void beforeNotify(Report report) {
                report.getError().setContext("Manual");
            }
        });
        apiClient.awaitReport();
        assertNotNull(apiClient.report);
        assertEquals("Manual", apiClient.report.getError().getContext());
    }

    static class FakeClient implements ErrorReportApiClient {

        CountDownLatch latch = new CountDownLatch(1);
        Report report;

        @Override
        public void postReport(String urlString,
                               Report report,
                               Map<String, String> headers)
            throws NetworkException, BadResponseException {
            this.report = report;
            latch.countDown();
        }

        void awaitReport() throws InterruptedException {
            latch.await(2000, TimeUnit.MILLISECONDS);
        }
    }

}
