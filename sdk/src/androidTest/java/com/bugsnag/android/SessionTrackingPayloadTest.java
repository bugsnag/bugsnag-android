package com.bugsnag.android;

import android.support.test.InstrumentationRegistry;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;

import static com.bugsnag.android.BugsnagTestUtils.generateSession;
import static com.bugsnag.android.BugsnagTestUtils.streamableToJson;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;

public class SessionTrackingPayloadTest {

    private JSONObject rootNode;
    private Session session;

    @Before
    public void setUp() throws Exception {
        session = generateSession();
        AppData appData = new AppData(InstrumentationRegistry.getContext(), new Configuration("a"), new SessionTracker());
        SessionTrackingPayload payload = new SessionTrackingPayload(Collections.singleton(session), appData);
        rootNode = streamableToJson(payload);
    }

    @Test
    public void testPayloadSerialisation() throws Exception {
        assertNotNull(rootNode);
        JSONArray sessions = rootNode.getJSONArray("sessions");

        JSONObject sessionNode = sessions.getJSONObject(0);
        assertNotNull(sessionNode);
        assertEquals("test", sessionNode.getString("id"));
        assertEquals(DateUtils.toISO8601(session.getStartedAt()), sessionNode.getString("startedAt"));
        assertNotNull(sessionNode.getJSONObject("user"));

        assertNotNull(rootNode.getJSONObject("notifier"));
        assertNotNull(rootNode.getJSONObject("device"));
        assertNotNull(rootNode.getJSONObject("app"));
    }

}
