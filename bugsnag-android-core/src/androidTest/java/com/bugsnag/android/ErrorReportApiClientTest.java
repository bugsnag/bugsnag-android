package com.bugsnag.android;

import static com.bugsnag.android.BugsnagTestUtils.generateClient;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import android.support.annotation.NonNull;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Map;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class ErrorReportApiClientTest {

    private FakeApiClient apiClient;
    private Client client;

    @Before
    public void setUp() throws Exception {
        apiClient = new FakeApiClient();
        client = generateClient();
    }

    @After
    public void tearDown() {
        client.close();
    }

    @SuppressWarnings("deprecation")
    @Test(expected = IllegalArgumentException.class)
    public void testApiClientNullValidation() {
        client.setErrorReportApiClient(null);
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testPostReportCalled() {
        client.setErrorReportApiClient(apiClient);

        assertNull(apiClient.report);
        client.notifyBlocking(new Throwable());
        assertNotNull(apiClient.report);
    }

    @SuppressWarnings("deprecation")
    private static class FakeApiClient implements ErrorReportApiClient {
        private Report report;

        @Override
        public void postReport(@NonNull String urlString,
                               @NonNull Report report,
                               @NonNull Map<String, String> headers)
            throws NetworkException, BadResponseException {
            this.report = report;
        }
    }
}
