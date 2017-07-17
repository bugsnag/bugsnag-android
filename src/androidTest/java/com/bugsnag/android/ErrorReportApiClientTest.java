package com.bugsnag.android;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

@RunWith(AndroidJUnit4.class)
public class ErrorReportApiClientTest {

    private FakeApiClient apiClient;
    private Context context;

    @Before
    public void setUp() throws Exception {
        context = InstrumentationRegistry.getContext();
        apiClient = new FakeApiClient();
        Bugsnag.init(context, "123");
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
