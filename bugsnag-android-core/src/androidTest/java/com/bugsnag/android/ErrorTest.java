package com.bugsnag.android;

import static com.bugsnag.android.BugsnagTestUtils.generateClient;
import static com.bugsnag.android.BugsnagTestUtils.generateSessionTracker;
import static com.bugsnag.android.BugsnagTestUtils.streamableToJson;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.SmallTest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Date;
import java.util.Map;

@SmallTest
public class ErrorTest {

    private Configuration config;
    private Error error;
    private Client client;

    /**
     * Generates a new default error for use by tests
     *
     * @throws Exception if initialisation failed
     */
    @Before
    public void setUp() throws Exception {
        config = new Configuration("api-key");
        RuntimeException exception = new RuntimeException("Example message");
        error = new Error.Builder(config, exception, generateSessionTracker(),
            Thread.currentThread(), false).build();
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
    public void testShouldIgnoreClass() {
        config.setIgnoreClasses(new String[]{"java.io.IOException"});

        // Shouldn't ignore classes not in ignoreClasses
        RuntimeException runtimeException = new RuntimeException("Test");
        Error error = new Error.Builder(config,
            runtimeException, generateSessionTracker(), Thread.currentThread(), false).build();
        assertFalse(error.shouldIgnoreClass());

        // Should ignore errors in ignoreClasses
        IOException ioException = new IOException("Test");
        error = new Error.Builder(config,
            ioException, generateSessionTracker(), Thread.currentThread(), false).build();
        assertTrue(error.shouldIgnoreClass());
    }

    @Test
    public void testBasicSerialization() throws JSONException, IOException {
        error.setAppData(client.getAppData().getAppData());

        JSONObject errorJson = streamableToJson(error);
        assertEquals("warning", errorJson.get("severity"));
        assertNotNull(errorJson.get("severity"));
        assertNotNull(errorJson.get("severityReason"));
        assertNotNull(errorJson.get("metaData"));
        assertNotNull(errorJson.get("threads"));
        assertNotNull(errorJson.get("exceptions"));
        assertNotNull(errorJson.get("app"));
        assertFalse(errorJson.has("failureReason"));
    }

    @Test
    public void testHandledSerialisation() throws Exception {
        Error err = new Error.Builder(config,
            new RuntimeException(), generateSessionTracker(), Thread.currentThread(), false)
            .severityReasonType(HandledState.REASON_HANDLED_EXCEPTION)
            .build();

        JSONObject errorJson = streamableToJson(err);
        assertNotNull(errorJson);
        assertEquals("warning", errorJson.getString("severity"));
        assertEquals(false, errorJson.getBoolean("unhandled"));

        JSONObject severityReason = errorJson.getJSONObject("severityReason");
        assertNotNull(severityReason);
        assertEquals(HandledState.REASON_HANDLED_EXCEPTION, severityReason.getString("type"));
        validateEmptyAttributes(severityReason);
    }

    @Test
    public void testUnhandledSerialisation() throws Exception {
        Error err = new Error.Builder(config,
            new RuntimeException(), generateSessionTracker(), Thread.currentThread(), false)
            .severityReasonType(HandledState.REASON_UNHANDLED_EXCEPTION)
            .severity(Severity.ERROR)
            .build();

        JSONObject errorJson = streamableToJson(err);
        assertNotNull(errorJson);
        assertEquals("error", errorJson.getString("severity"));
        assertEquals(true, errorJson.getBoolean("unhandled"));

        JSONObject severityReason = errorJson.getJSONObject("severityReason");
        assertNotNull(severityReason);
        assertEquals(HandledState.REASON_UNHANDLED_EXCEPTION, severityReason.getString("type"));
        validateEmptyAttributes(severityReason);
    }

    @Test
    public void testPromiseRejectionSerialisation() throws Exception {
        Error err = new Error.Builder(config,
            new RuntimeException(), generateSessionTracker(), Thread.currentThread(), false)
            .severityReasonType(HandledState.REASON_PROMISE_REJECTION)
            .severity(Severity.ERROR)
            .build();

        JSONObject errorJson = streamableToJson(err);
        assertNotNull(errorJson);
        assertEquals("error", errorJson.getString("severity"));
        assertEquals(true, errorJson.getBoolean("unhandled"));

        JSONObject severityReason = errorJson.getJSONObject("severityReason");
        assertNotNull(severityReason);
        assertEquals(HandledState.REASON_PROMISE_REJECTION, severityReason.getString("type"));
        validateEmptyAttributes(severityReason);
    }

    @Test
    public void testLogSerialisation() throws Exception {
        Error err = new Error.Builder(config,
            new RuntimeException(), generateSessionTracker(), Thread.currentThread(), false)
            .severityReasonType(HandledState.REASON_LOG)
            .severity(Severity.WARNING)
            .attributeValue("warning")
            .build();

        JSONObject errorJson = streamableToJson(err);
        assertNotNull(errorJson);
        assertEquals("warning", errorJson.getString("severity"));
        assertEquals(false, errorJson.getBoolean("unhandled"));

        JSONObject severityReason = errorJson.getJSONObject("severityReason");
        assertNotNull(severityReason);
        assertEquals(HandledState.REASON_LOG, severityReason.getString("type"));
        JSONObject attributes = severityReason.getJSONObject("attributes");
        assertNotNull(attributes);
        assertEquals(1, attributes.length());
        assertEquals("warning", attributes.getString("level"));
    }

    @Test
    public void testUserSpecifiedSerialisation() throws Exception {
        JSONObject errorJson = streamableToJson(error);
        assertNotNull(errorJson);
        assertEquals("warning", errorJson.getString("severity"));
        assertEquals(false, errorJson.getBoolean("unhandled"));

        JSONObject severityReason = errorJson.getJSONObject("severityReason");
        assertNotNull(severityReason);
        assertEquals(HandledState.REASON_USER_SPECIFIED, severityReason.getString("type"));
        validateEmptyAttributes(severityReason);
    }

    @Test
    public void testStrictModeSerialisation() throws Exception {
        Error err = new Error.Builder(config,
            new RuntimeException(), generateSessionTracker(), Thread.currentThread(), false)
            .severityReasonType(HandledState.REASON_STRICT_MODE)
            .attributeValue("Test")
            .build();

        JSONObject errorJson = streamableToJson(err);
        assertNotNull(errorJson);
        assertEquals("warning", errorJson.getString("severity"));
        assertEquals(true, errorJson.getBoolean("unhandled"));

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
        error.setSeverity(Severity.INFO); // mutate severity

        JSONObject errorJson = streamableToJson(error);
        assertNotNull(errorJson);
        assertEquals("info", errorJson.getString("severity"));
        assertEquals(false, errorJson.getBoolean("unhandled"));

        JSONObject severityReason = errorJson.getJSONObject("severityReason");
        assertNotNull(severityReason);
        assertEquals(HandledState.REASON_CALLBACK_SPECIFIED, severityReason.getString("type"));
        validateEmptyAttributes(severityReason);
    }

    @Test
    public void testSetContext() throws JSONException, IOException {
        String context = "ExampleContext";
        error.setContext(context);

        JSONObject errorJson = streamableToJson(error);
        assertEquals(context, errorJson.get("context"));
    }

    @Test
    public void testSetGroupingHash() throws JSONException, IOException {
        String groupingHash = "herpderp";
        error.setGroupingHash(groupingHash);
        assertEquals(groupingHash, error.getGroupingHash());

        JSONObject errorJson = streamableToJson(error);
        assertEquals(groupingHash, errorJson.get("groupingHash"));
    }

    @Test
    public void testSetSeverity() throws JSONException, IOException {
        error.setSeverity(Severity.INFO);

        JSONObject errorJson = streamableToJson(error);
        assertEquals("info", errorJson.get("severity"));
    }

    @Test
    public void testSessionIncluded() throws Exception {
        SessionTracker sessionTracker = generateSessionTracker();
        final Session session = sessionTracker.startNewSession(new Date(), new User(), false);
        Error err = new Error.Builder(config,
            new RuntimeException(), sessionTracker, Thread.currentThread(), false).build();

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
        Error err = new Error.Builder(config,
            new RuntimeException(), generateSessionTracker(),
            Thread.currentThread(), false).build();

        JSONObject errorJson = streamableToJson(err);
        assertNotNull(errorJson);
        errorJson.getJSONObject("session"); // session should not be serialised
    }

    @Test
    public void checkExceptionMessageNullity() throws Exception {
        String msg = "Foo";
        Error err = new Error.Builder(config,
            new RuntimeException(msg), generateSessionTracker(),
            Thread.currentThread(), false).build();
        assertEquals(msg, err.getExceptionMessage());

        err = new Error.Builder(config,
            new RuntimeException(), generateSessionTracker(),
            Thread.currentThread(), false).build();
        assertEquals("", err.getExceptionMessage());
    }

    @Test
    public void testNullSeverity() throws Exception {
        error.setSeverity(null);
        assertEquals(Severity.WARNING, error.getSeverity());
    }

    @Test
    public void testSendThreadsDisabled() throws Exception {
        config.setSendThreads(false);
        JSONObject errorJson = streamableToJson(error);
        assertFalse(errorJson.has("threads"));
    }

    @Test
    public void testBugsnagExceptionName() throws Exception {
        BugsnagException exception = new BugsnagException("Busgang", "exceptional",
            new StackTraceElement[]{});
        Error err = new Error.Builder(config,
            exception, generateSessionTracker(), Thread.currentThread(), false).build();
        assertEquals("Busgang", err.getExceptionName());
    }

    @Test
    public void testNullContext() throws Exception {
        error.setContext(null);
        error.setAppData(null);
        assertNull(error.getContext());
    }

    @Test
    public void testSetUser() throws Exception {
        String firstId = "123";
        String firstEmail = "fake@example.com";
        String firstName = "Bob Swaggins";
        error.setUser(firstId, firstEmail, firstName);

        assertEquals(firstId, error.getUser().getId());
        assertEquals(firstEmail, error.getUser().getEmail());
        assertEquals(firstName, error.getUser().getName());

        String userId = "foo";
        error.setUserId(userId);
        assertEquals(userId, error.getUser().getId());
        assertEquals(firstEmail, error.getUser().getEmail());
        assertEquals(firstName, error.getUser().getName());

        String userEmail = "another@example.com";
        error.setUserEmail(userEmail);
        assertEquals(userId, error.getUser().getId());
        assertEquals(userEmail, error.getUser().getEmail());
        assertEquals(firstName, error.getUser().getName());

        String userName = "Isaac";
        error.setUserName(userName);
        assertEquals(userId, error.getUser().getId());
        assertEquals(userEmail, error.getUser().getEmail());
        assertEquals(userName, error.getUser().getName());
    }

    @Test
    public void testBuilderMetaData() {
        Configuration config = new Configuration("api-key");
        Error.Builder builder = new Error.Builder(config,
            new RuntimeException("foo"), generateSessionTracker(),
            Thread.currentThread(), false);

        assertNotNull(builder.metaData(new MetaData()).build());

        MetaData metaData = new MetaData();
        metaData.addToTab("foo", "bar", true);

        Error error = builder.metaData(metaData).build();
        assertEquals(1, error.getMetaData().getTab("foo").size());
    }

    @Test
    public void testErrorMetaData() {
        error.addToTab("rocks", "geode", "a shiny mineral");
        assertNotNull(error.getMetaData().getTab("rocks"));

        error.clearTab("rocks");
        assertTrue(error.getMetaData().getTab("rocks").isEmpty());
    }

    @Test
    public void testSetDeviceId() throws Throwable {
        Connectivity connectivity = BugsnagTestUtils.generateConnectivity();
        Context context = ApplicationProvider.getApplicationContext();
        Resources resources = context.getResources();
        SharedPreferences prefs = context.getSharedPreferences("", Context.MODE_PRIVATE);
        DeviceData data = new DeviceData(connectivity, context, resources, prefs);

        Map<String, Object> deviceData = data.getDeviceData();
        error.setDeviceData(deviceData);
        assertEquals(deviceData, error.getDeviceData());

        JSONObject errorJson = streamableToJson(error);
        JSONObject device = errorJson.getJSONObject("device");
        assertEquals(deviceData.get("id"), device.getString("id"));

        error.setDeviceId(null);
        errorJson = streamableToJson(error);
        device = errorJson.getJSONObject("device");
        assertFalse(device.has("id"));
    }

    @Test
    public void testBuilderNullSession() throws Throwable {
        Configuration config = new Configuration("api-key");
        config.setAutoCaptureSessions(false);
        RuntimeException exception = new RuntimeException("foo");

        SessionTracker sessionTracker = generateSessionTracker();
        sessionTracker.startNewSession(new Date(), new User(), true);
        error = new Error.Builder(config, exception, sessionTracker,
            Thread.currentThread(), false).build();

        JSONObject errorJson = streamableToJson(error);
        assertFalse(errorJson.has("session"));
    }

    @Test
    public void testFailureReason() throws JSONException, IOException {
        error.addFailureReason("Something went wrong");
        error.addFailureReason("Whoops!");
        JSONObject json = streamableToJson(error);
        JSONArray failureReason = json.getJSONArray("failureReason");
        assertEquals("Something went wrong", failureReason.get(0));
        assertEquals("Whoops!", failureReason.get(1));
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
