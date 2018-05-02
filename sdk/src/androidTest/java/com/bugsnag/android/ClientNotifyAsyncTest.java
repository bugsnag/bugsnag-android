package com.bugsnag.android;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
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
public class ClientNotifyAsyncTest {

    private Client client;
    private NullCheckClient apiClient;

    /**
     * Generates a configuration and clears sharedPrefs values to begin the test with a clean slate
     *
     * @throws Exception if initialisation failed
     */
    @Before
    public void setUp() throws Exception {
        client = BugsnagTestUtils.generateClient();
        apiClient = new NullCheckClient();
        client.setErrorReportApiClient(apiClient);
    }

    @Test
    public void testNotifyAsyncMetadata() throws Exception {
        MetaData metaData = new MetaData();
        metaData.addToTab("animals", "dog", true);

        client.notify(new RuntimeException("Foo"), metaData);
        assertNull(apiClient.report);
        apiClient.nullCheckLatch.countDown();

        apiClient.awaitReport();
        assertNotNull(apiClient.report);
        MetaData data = apiClient.report.getError().getMetaData();
        assertTrue((Boolean) data.getTab("animals").get("dog"));
    }

    @Test
    public void testNotifyAsyncSeverity() throws Exception {
        client.notify(new RuntimeException("Foo"), Severity.INFO);
        assertNull(apiClient.report);
        apiClient.nullCheckLatch.countDown();

        apiClient.awaitReport();
        assertNotNull(apiClient.report);
        assertEquals(Severity.INFO, apiClient.report.getError().getSeverity());
    }

    @Test
    public void testNotifyAsyncSeverityMetadata() throws Exception {
        MetaData metaData = new MetaData();
        metaData.addToTab("animals", "bird", "chicken");

        client.notify(new RuntimeException("Foo"), Severity.ERROR, metaData);
        assertNull(apiClient.report);
        apiClient.nullCheckLatch.countDown();

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
        assertNull(apiClient.report);
        apiClient.nullCheckLatch.countDown();

        apiClient.awaitReport();
        assertNotNull(apiClient.report);
        assertEquals("Manual", apiClient.report.getError().getContext());
    }

    static class NullCheckClient extends ClientNotifyTest.FakeClient {

        CountDownLatch nullCheckLatch = new CountDownLatch(1);

        @Override
        public void postReport(String urlString,
                               Report report,
                               Map<String, String> headers)
            throws NetworkException, BadResponseException {
            try {
                nullCheckLatch.await(20, TimeUnit.MILLISECONDS);
            } catch (InterruptedException exception) {
                exception.printStackTrace();
            }
            super.postReport(urlString, report, headers);
        }
    }

}
