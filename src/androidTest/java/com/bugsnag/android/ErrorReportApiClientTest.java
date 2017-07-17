package com.bugsnag.android;

import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class ErrorReportApiClientTest {

    private FakeApiClient apiClient;

    @Before
    public void setUp() throws Exception {
        apiClient = new FakeApiClient();
        Bugsnag.init(InstrumentationRegistry.getContext(), "123");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testApiClientNullValidation() {
        Bugsnag.setErrorReportApiClient(null);
    }

    @Test
    public void testPostReportCalled() {
        Bugsnag.setErrorReportApiClient(apiClient);

        assertNull(apiClient.report);
        Client client = Bugsnag.getClient();
        client.notifyBlocking(new Throwable());
        assertNotNull(apiClient.report);
    }

    private static class FakeApiClient implements ErrorReportApiClient {
        private Report report;

        @Override
        public void postReport(String urlString, Report report) throws NetworkException, BadResponseException {
            this.report = report;
        }
    }
}
