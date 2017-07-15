package com.bugsnag.android;

public class ErrorReportApiClientTest extends BugsnagTestCase {

    private FakeApiClient apiClient;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        apiClient = new FakeApiClient();
    }

    public void testApiClientNullValidation() {
        Bugsnag.init(getContext(), "123");
        try {
            Bugsnag.setErrorReportApiClient(null);
            fail("ErrorReportApiClient cannot be null");
        }
        catch (IllegalArgumentException ignored) {
        }
    }

    public void testPostReportCalled() {
        Bugsnag.init(getContext(), "123");
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
