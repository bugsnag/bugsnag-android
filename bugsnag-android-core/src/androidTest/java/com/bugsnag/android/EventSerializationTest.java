package com.bugsnag.android;

import static com.bugsnag.android.BugsnagTestUtils.convert;
import static com.bugsnag.android.BugsnagTestUtils.generateClient;
import static com.bugsnag.android.BugsnagTestUtils.generateSessionTracker;
import static com.bugsnag.android.BugsnagTestUtils.streamableToJson;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.SmallTest;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.Map;

@SmallTest
public class EventSerializationTest {

    private HandledState handledState
            = HandledState.newInstance(HandledState.REASON_HANDLED_EXCEPTION);
    private ImmutableConfig config;
    private Event event;
    private Client client;

    /**
     * Generates a new default event for use by tests
     *
     */
    @Before
    public void setUp() {
        Configuration configuration = BugsnagTestUtils.generateConfiguration();
        this.config = convert(configuration);
        RuntimeException exception = new RuntimeException("Example message");
        event = new Event(exception, config, handledState);
        client = generateClient();
    }

    /**
     * Tears down the client
     */
    @After
    public void tearDown() {
        if (client != null) {
            client.close();
        }
    }

    @Test
    public void testBasicSerialization() throws JSONException, IOException {
        event.setApp(client.appData.getAppData());

        JSONObject errorJson = streamableToJson(event);
        assertEquals("warning", errorJson.get("severity"));
        assertNotNull(errorJson.get("severity"));
        assertNotNull(errorJson.get("severityReason"));
        assertNotNull(errorJson.get("metaData"));
        assertNotNull(errorJson.get("threads"));
        assertNotNull(errorJson.get("exceptions"));
        assertNotNull(errorJson.get("app"));
    }

    @Test
    public void testHandledSerialisation() throws Exception {
        Event err = new Event(new RuntimeException(), config, handledState);
        JSONObject errorJson = streamableToJson(err);
        assertNotNull(errorJson);
        assertEquals("warning", errorJson.getString("severity"));
        assertFalse(errorJson.getBoolean("unhandled"));

        JSONObject severityReason = errorJson.getJSONObject("severityReason");
        assertNotNull(severityReason);
        assertEquals(HandledState.REASON_HANDLED_EXCEPTION, severityReason.getString("type"));
        validateEmptyAttributes(severityReason);
    }

    @Test
    public void testUnhandledSerialisation() throws Exception {
        handledState = HandledState.newInstance(HandledState.REASON_UNHANDLED_EXCEPTION);
        Event err = new Event(new RuntimeException(), config, handledState);

        JSONObject errorJson = streamableToJson(err);
        assertNotNull(errorJson);
        assertEquals("error", errorJson.getString("severity"));
        assertTrue(errorJson.getBoolean("unhandled"));

        JSONObject severityReason = errorJson.getJSONObject("severityReason");
        assertNotNull(severityReason);
        assertEquals(HandledState.REASON_UNHANDLED_EXCEPTION, severityReason.getString("type"));
        validateEmptyAttributes(severityReason);
    }

    @Test
    public void testPromiseRejectionSerialisation() throws Exception {
        handledState = HandledState.newInstance(HandledState.REASON_PROMISE_REJECTION);
        Event err = new Event(new RuntimeException(), config, handledState);

        JSONObject errorJson = streamableToJson(err);
        assertNotNull(errorJson);
        assertEquals("error", errorJson.getString("severity"));
        assertTrue(errorJson.getBoolean("unhandled"));

        JSONObject severityReason = errorJson.getJSONObject("severityReason");
        assertNotNull(severityReason);
        assertEquals(HandledState.REASON_PROMISE_REJECTION, severityReason.getString("type"));
        validateEmptyAttributes(severityReason);
    }

    @Test
    public void testLogSerialisation() throws Exception {
        handledState = HandledState.newInstance(HandledState.REASON_LOG,
                Severity.WARNING, "warning");
        Event err = new Event(new RuntimeException(), config, handledState);

        JSONObject errorJson = streamableToJson(err);
        assertNotNull(errorJson);
        assertEquals("warning", errorJson.getString("severity"));
        assertFalse(errorJson.getBoolean("unhandled"));

        JSONObject severityReason = errorJson.getJSONObject("severityReason");
        assertNotNull(severityReason);
        assertEquals(HandledState.REASON_LOG, severityReason.getString("type"));
        JSONObject attributes = severityReason.getJSONObject("attributes");
        assertNotNull(attributes);
        assertEquals(1, attributes.length());
        assertEquals("warning", attributes.getString("level"));
    }

    @Test
    public void testStrictModeSerialisation() throws Exception {
        handledState = HandledState.newInstance(HandledState.REASON_STRICT_MODE,
                Severity.WARNING, "Test");
        Event err = new Event(new RuntimeException(), config, handledState);

        JSONObject errorJson = streamableToJson(err);
        assertNotNull(errorJson);
        assertEquals("warning", errorJson.getString("severity"));
        assertTrue(errorJson.getBoolean("unhandled"));

        JSONObject severityReason = errorJson.getJSONObject("severityReason");
        assertNotNull(severityReason);
        assertEquals(HandledState.REASON_STRICT_MODE, severityReason.getString("type"));

        JSONObject attributes = severityReason.getJSONObject("attributes");
        assertNotNull(attributes);
        assertEquals(1, attributes.length());
        assertEquals("Test", attributes.getString("violationType"));
    }

    @Test
    public void testCallbackSpecified() throws Exception {
        event.setSeverity(Severity.INFO); // mutate severity

        JSONObject errorJson = streamableToJson(event);
        assertNotNull(errorJson);
        assertEquals("info", errorJson.getString("severity"));
        assertFalse(errorJson.getBoolean("unhandled"));

        JSONObject severityReason = errorJson.getJSONObject("severityReason");
        assertNotNull(severityReason);
        assertEquals(HandledState.REASON_CALLBACK_SPECIFIED, severityReason.getString("type"));
        validateEmptyAttributes(severityReason);
    }

    @Test
    public void testSetContext() throws JSONException, IOException {
        String context = "ExampleContext";
        event.setContext(context);

        JSONObject errorJson = streamableToJson(event);
        assertEquals(context, errorJson.get("context"));
    }

    @Test
    public void testSetGroupingHash() throws JSONException, IOException {
        String groupingHash = "herpderp";
        event.setGroupingHash(groupingHash);
        assertEquals(groupingHash, event.getGroupingHash());

        JSONObject errorJson = streamableToJson(event);
        assertEquals(groupingHash, errorJson.get("groupingHash"));
    }

    @Test
    public void testSetSeverity() throws JSONException, IOException {
        event.setSeverity(Severity.INFO);

        JSONObject errorJson = streamableToJson(event);
        assertEquals("info", errorJson.get("severity"));
    }

    @Test
    public void testSessionIncluded() throws Exception {
        SessionTracker sessionTracker = generateSessionTracker();
        User user = new User(null, null, null);
        final Session session = sessionTracker.startNewSession(new Date(), user, false);
        Event err = new Event(new RuntimeException(), config, handledState);
        err.setSession(session.incrementHandledAndCopy());


        JSONObject errorJson = streamableToJson(err);
        assertNotNull(errorJson);

        JSONObject sessionNode = errorJson.getJSONObject("session");
        assertNotNull(sessionNode);
        assertEquals(3, sessionNode.length());
        assertEquals(session.getId(), sessionNode.getString("id"));
        String startedAt = sessionNode.getString("startedAt");
        assertEquals(DateUtils.toIso8601(session.getStartedAt()), startedAt);

        JSONObject eventsNode = sessionNode.getJSONObject("events");
        assertNotNull(eventsNode);
        assertEquals(2, eventsNode.length());
        assertEquals(1, eventsNode.get("handled"));
    }

    @Test(expected = JSONException.class)
    public void testSessionExcluded() throws Exception {
        Event err = new Event(new RuntimeException(), config, handledState);
        JSONObject errorJson = streamableToJson(err);
        assertNotNull(errorJson);
        errorJson.getJSONObject("session"); // session should not be serialised
    }

    @Test
    public void testSendThreadsDisabled() throws Exception {
        Configuration configuration = BugsnagTestUtils.generateConfiguration();
        configuration.setSendThreads(false);
        Event err = new Event(new RuntimeException(), convert(configuration), handledState);
        JSONObject errorJson = streamableToJson(err);
        assertFalse(errorJson.has("threads"));
    }

    @Test
    public void testSetDeviceId() throws Throwable {
        Connectivity connectivity = BugsnagTestUtils.generateConnectivity();
        Context context = ApplicationProvider.getApplicationContext();
        Resources resources = context.getResources();
        DeviceData data = new DeviceData(connectivity, context, resources,
                "123", NoopLogger.INSTANCE);

        Map<String, Object> deviceData = data.getDeviceData();
        event.setDevice(deviceData);
        assertEquals(deviceData, event.getDevice());

        JSONObject errorJson = streamableToJson(event);
        JSONObject device = errorJson.getJSONObject("device");
        assertEquals(deviceData.get("id"), device.getString("id"));

        event.getDevice().remove("id");
        errorJson = streamableToJson(event);
        device = errorJson.getJSONObject("device");
        assertFalse(device.has("id"));
    }

    @Test
    public void testBuilderNullSession() throws Throwable {
        Configuration config = BugsnagTestUtils.generateConfiguration();
        config.setAutoTrackSessions(false);
        RuntimeException exception = new RuntimeException("foo");

        SessionTracker sessionTracker = generateSessionTracker();
        sessionTracker.startNewSession(new Date(), new User(null, null, null), true);
        event = new Event(exception, BugsnagTestUtils.convert(config), handledState);

        JSONObject errorJson = streamableToJson(event);
        assertFalse(errorJson.has("session"));
    }

    @Test
    public void shouldIgnoreEmpty() {
        Configuration configuration = BugsnagTestUtils.generateConfiguration();
        configuration.setIgnoreClasses(Collections.<String>emptySet());
        ImmutableConfig conf = convert(configuration);
        event = new Event(new RuntimeException(), conf, handledState);
        assertFalse(event.shouldIgnoreClass());
    }

    @Test
    public void shouldIgnoreMatches() {
        Configuration configuration = BugsnagTestUtils.generateConfiguration();
        configuration.setIgnoreClasses(Collections.singleton("java.io.IOException"));

        event = new Event(new IOException(), BugsnagTestUtils.convert(configuration), handledState);
        assertTrue(event.shouldIgnoreClass());
    }

    private void validateEmptyAttributes(JSONObject severityReason) {
        try {
            severityReason.getJSONObject("attributes");
            fail();
        } catch (JSONException ignored) {
            Assert.assertNotNull(ignored);
        }
    }
}
