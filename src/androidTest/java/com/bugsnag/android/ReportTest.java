package com.bugsnag.android;

import java.io.IOException;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;

public class ReportTest extends BugsnagTestCase {
    public void testInMemoryError() throws JSONException, IOException {
        Configuration config = new Configuration("example-api-key");
        Error error = new Error(config, new RuntimeException("Something broke"));
        Report report = new Report(config.getApiKey(), error);

        JSONObject reportJson = streamableToJson(report);
        assertEquals("example-api-key", reportJson.get("apiKey"));
        assertEquals(1, reportJson.getJSONArray("events").length());
    }

    public void testModifyingAPIKey() throws JSONException, IOException {
        Configuration config = new Configuration("example-api-key");
        Error error = new Error(config, new RuntimeException("Something broke"));
        Report report = new Report(config.getApiKey(), error);
        report.setApiKey("other-api-key");

        JSONObject reportJson = streamableToJson(report);
        assertEquals("other-api-key", reportJson.get("apiKey"));
    }

    public void testModifyingGroupingHash() throws JSONException, IOException {
        Configuration config = new Configuration("example-api-key");
        Error error = new Error(config, new RuntimeException("Something broke"));
        Report report = new Report(config.getApiKey(), error);
        report.getError().setGroupingHash("File.java:300429");

        JSONObject reportJson = streamableToJson(report);
        JSONArray events = reportJson.getJSONArray("events");
        JSONObject event = events.getJSONObject(0);
        assertEquals("File.java:300429", event.getString("groupingHash"));
    }
}
