package com.bugsnag.android;

import static org.junit.Assert.assertEquals;

import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.After;
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
    @SuppressWarnings("deprecation")
    @Before
    public void setUp() throws Exception {
        client = BugsnagTestUtils.generateClient();
        apiClient = new FakeClient();
        client.setErrorReportApiClient(apiClient);
    }

    @After
    public void tearDown() throws Exception {
        Async.cancelTasks();
        client.getOrientationListener().disable();
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

    @SuppressWarnings("deprecation")
    static class FakeClient implements ErrorReportApiClient {

        CountDownLatch latch = new CountDownLatch(1);
        Report report;

        @Override
        public void postReport(String urlString,
                               Report report,
                               Map<String, String> headers)
            throws NetworkException, BadResponseException {
            try {
                Thread.sleep(10); // simulate async request
            } catch (InterruptedException ignored) {
                ignored.printStackTrace();
            }
            this.report = report;
            latch.countDown();
        }

        void awaitReport() throws InterruptedException {
            latch.await(2000, TimeUnit.MILLISECONDS);
        }
    }

}
