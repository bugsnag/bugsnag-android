package com.bugsnag.android;

import static com.bugsnag.android.BugsnagTestUtils.streamableToJson;
import static org.junit.Assert.assertEquals;

import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class ReportTest {

    private Report report;

    /**
     * Generates a report
     *
     * @throws Exception if initialisation failed
     */
    @Before
    public void setUp() throws Exception {
        Configuration config = new Configuration("example-api-key");
        RuntimeException exception = new RuntimeException("Something broke");
        Error error = new Error.Builder(config, exception,
            BugsnagTestUtils.generateSessionTracker(),
            Thread.currentThread(), false).build();
        report = new Report("api-key", error);
    }

    @Test
    public void testInMemoryError() throws JSONException, IOException {
        JSONObject reportJson = streamableToJson(report);
        assertEquals(1, reportJson.getJSONArray("events").length());
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

    @Test
    public void testModifyReportDetails() throws Exception {
        report.setApiKey("custom-api-key");
        report.getNotifier().setName("React Native");
        report.getNotifier().setURL("https://bugsnag.com/reactnative");
        report.getNotifier().setVersion("3.4.5");

        JSONObject reportJson = streamableToJson(report);
        assertEquals("custom-api-key", reportJson.getString("apiKey"));

        JSONObject notifier = reportJson.getJSONObject("notifier");
        assertEquals("React Native", notifier.getString("name"));
        assertEquals("3.4.5", notifier.getString("version"));
        assertEquals("https://bugsnag.com/reactnative", notifier.getString("url"));
    }

}
