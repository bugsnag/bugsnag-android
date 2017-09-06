package com.bugsnag.android;

import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;

import static com.bugsnag.android.BugsnagTestUtils.streamableToJson;
import static org.junit.Assert.assertEquals;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class ReportTest {

    private Report report;

    @Before
    public void setUp() throws Exception {
        Configuration config = new Configuration("example-api-key");
        Error error = new Error.Builder(config, new RuntimeException("Something broke")).build();
        report = new Report(config.getApiKey(), error);
    }

    @Test
    public void testInMemoryError() throws JSONException, IOException {
        JSONObject reportJson = streamableToJson(report);
        assertEquals("example-api-key", reportJson.get("apiKey"));
        assertEquals(1, reportJson.getJSONArray("events").length());
    }

    @Test
    public void testModifyingAPIKey() throws JSONException, IOException {
        String apiKey = "other-api-key";
        report.setApiKey(apiKey);

        JSONObject reportJson = streamableToJson(report);
        assertEquals(apiKey, reportJson.get("apiKey"));
    }

    @Test
    public void testModifyingGroupingHash() throws JSONException, IOException {
        String groupingHash = "File.java:300429";
        report.getError().setGroupingHash(groupingHash);

        JSONObject reportJson = streamableToJson(report);
        JSONArray events = reportJson.getJSONArray("events");
        JSONObject event = events.getJSONObject(0);
        assertEquals(groupingHash, event.getString("groupingHash"));
    }
}
