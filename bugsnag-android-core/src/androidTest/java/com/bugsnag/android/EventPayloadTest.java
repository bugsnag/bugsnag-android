package com.bugsnag.android;

import static com.bugsnag.android.BugsnagTestUtils.generateAppWithState;
import static com.bugsnag.android.BugsnagTestUtils.generateDeviceWithState;
import static com.bugsnag.android.BugsnagTestUtils.streamableToJson;
import static org.junit.Assert.assertEquals;

import com.bugsnag.android.internal.ImmutableConfig;

import androidx.test.filters.SmallTest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

@SmallTest
public class EventPayloadTest {

    private EventPayload eventPayload;

    /**
     * Generates a eventPayload
     */
    @Before
    public void setUp() {
        ImmutableConfig config = BugsnagTestUtils.generateImmutableConfig();
        RuntimeException exception = new RuntimeException("Something broke");
        SeverityReason severityReason = SeverityReason.newInstance(SeverityReason.REASON_ANR);
        Event event = new Event(exception, config, severityReason, NoopLogger.INSTANCE);
        event.setApp(generateAppWithState());
        event.setDevice(generateDeviceWithState());
        eventPayload = new EventPayload("api-key", event, new Notifier(), config);
    }

    @Test
    public void testInMemoryError() throws JSONException, IOException {
        JSONObject reportJson = streamableToJson(eventPayload);
        assertEquals(1, reportJson.getJSONArray("events").length());
    }

    @Test
    public void testModifyingGroupingHash() throws JSONException, IOException {
        String groupingHash = "File.java:300429";
        eventPayload.getEvent().setGroupingHash(groupingHash);

        JSONObject reportJson = streamableToJson(eventPayload);
        JSONArray events = reportJson.getJSONArray("events");
        JSONObject event = events.getJSONObject(0);
        assertEquals(groupingHash, event.getString("groupingHash"));
    }

}
