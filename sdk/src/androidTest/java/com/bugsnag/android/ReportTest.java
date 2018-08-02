package com.bugsnag.android;

import static com.bugsnag.android.BugsnagTestUtils.streamableToJson;
import static org.junit.Assert.assertEquals;

import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
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
        Error error = new Error.Builder(config, exception, null).build();
        report = new Report("api-key", error);
    }

    @After
    public void tearDown() throws Exception {
        Async.cancelTasks();
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

    @SuppressWarnings("deprecation")
    @Test
    public void testModifyReportDetails() throws Exception {
        String apiKey = "custom-api-key";
        String notifierName = "React Native";
        String notifierUrl = "https://bugsnag.com/reactnative";
        String notifierVersion = "3.4.5";

        report.setApiKey(apiKey);
        report.setNotifierName(notifierName);
        report.setNotifierURL(notifierUrl);
        report.setNotifierVersion(notifierVersion);

        JSONObject reportJson = streamableToJson(report);
        assertEquals(apiKey, reportJson.getString("apiKey"));

        JSONObject notifier = reportJson.getJSONObject("notifier");
        assertEquals(notifierName, notifier.getString("name"));
        assertEquals(notifierVersion, notifier.getString("version"));
        assertEquals(notifierUrl, notifier.getString("url"));
    }

}
