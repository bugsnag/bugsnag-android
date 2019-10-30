package com.bugsnag.android;

import static com.bugsnag.android.BugsnagTestUtils.streamableToJson;
import static org.junit.Assert.assertEquals;

import androidx.test.filters.SmallTest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

@SmallTest
public class ReportTest {

    private Report report;

    /**
     * Generates a report
     *
     */
    @Before
    public void setUp() {
        ImmutableConfig config = BugsnagTestUtils.generateImmutableConfig();
        RuntimeException exception = new RuntimeException("Something broke");
        HandledState handledState = HandledState.newInstance(HandledState.REASON_HANDLED_EXCEPTION);
        Event event = new Event(exception, config, handledState);
        report = new Report("api-key", null, event);
    }

    @Test
    public void testInMemoryError() throws JSONException, IOException {
        JSONObject reportJson = streamableToJson(report);
        assertEquals(1, reportJson.getJSONArray("events").length());
    }

    @Test
    public void testModifyingGroupingHash() throws JSONException, IOException {
        String groupingHash = "File.java:300429";
        report.getEvent().setGroupingHash(groupingHash);

        JSONObject reportJson = streamableToJson(report);
        JSONArray events = reportJson.getJSONArray("events");
        JSONObject event = events.getJSONObject(0);
        assertEquals(groupingHash, event.getString("groupingHash"));
    }

    @Test
    public void testModifyReportDetails() throws Exception {
        report.setApiKey("custom-api-key");
        Notifier.INSTANCE.setName("React Native");
        Notifier.INSTANCE.setUrl("https://bugsnag.com/reactnative");
        Notifier.INSTANCE.setVersion("3.4.5");

        JSONObject reportJson = streamableToJson(report);
        assertEquals("custom-api-key", reportJson.getString("apiKey"));

        JSONObject notifier = reportJson.getJSONObject("notifier");
        assertEquals("React Native", notifier.getString("name"));
        assertEquals("3.4.5", notifier.getString("version"));
        assertEquals("https://bugsnag.com/reactnative", notifier.getString("url"));
    }

}
