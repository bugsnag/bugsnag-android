package com.bugsnag.android;

import android.support.test.runner.AndroidJUnit4;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;

import static com.bugsnag.android.BugsnagTestUtils.streamableToJson;
import static org.junit.Assert.assertEquals;

@RunWith(AndroidJUnit4.class)
public class ReportTest {

    @Test
    public void testInMemoryError() throws JSONException, IOException {
        Configuration config = new Configuration("example-api-key");
        Error error = new Error(config, new RuntimeException("Something broke"));
        Report report = new Report(config.getApiKey(), error);

        JSONObject reportJson = streamableToJson(report);
        assertEquals("example-api-key", reportJson.get("apiKey"));
        assertEquals(1, reportJson.getJSONArray("events").length());
    }

    @Test
    public void testModifyingAPIKey() throws JSONException, IOException {
        Configuration config = new Configuration("example-api-key");
        Error error = new Error(config, new RuntimeException("Something broke"));
        Report report = new Report(config.getApiKey(), error);
        report.setApiKey("other-api-key");

        JSONObject reportJson = streamableToJson(report);
        assertEquals("other-api-key", reportJson.get("apiKey"));
    }

    @Test
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
