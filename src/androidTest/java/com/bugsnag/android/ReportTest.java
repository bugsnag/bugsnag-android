package com.bugsnag.android;

import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;

public class ReportTest extends BugsnagTestCase {
    public void testInMemoryError() throws JSONException, IOException {
        Configuration config = new Configuration("example-api-key");
        Report report = new Report(config);
        report.addError(new Error(config, new RuntimeException("Something broke")));

        JSONObject reportJson = streamableToJson(report);
        assertEquals("example-api-key", reportJson.get("apiKey"));
        assertEquals(1, reportJson.getJSONArray("events").length());
    }
}
