package com.bugsnag.android;

public class ReportApiClientTest extends BugsnagTestCase {

    private ReportApiClient reportApiClient;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        reportApiClient = new DefaultHttpClient();
    }

    public void testBugsnagNullValidation() {
        Bugsnag.init(getContext(), "123");
        try {
            Bugsnag.setReportApiClient(null);
            fail("ReportApiClient cannot be null");
        }
        catch (Exception ignored) {
        }
    }

    public void testBugsnagClient() {
        Bugsnag.init(getContext(), "123");
        Bugsnag.setReportApiClient(reportApiClient);
    }

}
