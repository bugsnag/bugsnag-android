package com.bugsnag.android;

public class ErrorReportApiClientTest extends BugsnagTestCase {

    private ErrorReportApiClient errorReportApiClient;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        errorReportApiClient = new DefaultHttpClient();
    }

    public void testBugsnagNullValidation() {
        Bugsnag.init(getContext(), "123");
        try {
            Bugsnag.setReportApiClient(null);
            fail("ErrorReportApiClient cannot be null");
        }
        catch (Exception ignored) {
        }
    }

    public void testBugsnagClient() {
        Bugsnag.init(getContext(), "123");
        Bugsnag.setReportApiClient(errorReportApiClient);
    }

}
