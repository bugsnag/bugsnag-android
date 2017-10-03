package com.bugsnag.android;

import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;

import static com.bugsnag.android.BugsnagTestUtils.streamableToJson;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class ErrorTest {

    private Configuration config;
    private Error error;

    @Before
    public void setUp() throws Exception {
        config = new Configuration("api-key");
        error = new Error.Builder(config, new RuntimeException("Example message")).build();
    }

    @Test
    public void testShouldIgnoreClass() {
        config.setIgnoreClasses(new String[]{"java.io.IOException"});

        // Shouldn't ignore classes not in ignoreClasses
        Error error = new Error.Builder(config, new RuntimeException("Test")).build();
        assertFalse(error.shouldIgnoreClass());

        // Should ignore errors in ignoreClasses
        error = new Error.Builder(config, new java.io.IOException("Test")).build();
        assertTrue(error.shouldIgnoreClass());
    }

    @Test
    public void testGetExceptionName() {
        assertEquals("java.lang.RuntimeException", error.getExceptionName());
    }

    @Test
    public void testGetExceptionMessage() {
        assertEquals("Example message", error.getExceptionMessage());
    }

    @Test
    public void testBasicSerialization() throws JSONException, IOException {
        JSONObject errorJson = streamableToJson(error);
        assertEquals("warning", errorJson.get("severity"));

        assertEquals("3", errorJson.get("payloadVersion"));
        assertNotNull(errorJson.get("severity"));
        assertNotNull(errorJson.get("metaData"));
        assertNotNull(errorJson.get("threads"));
    }

    @Test
    public void testHandledSerialisation() throws Exception {
        Error err = new Error.Builder(config, new RuntimeException())
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
        Error err = new Error.Builder(config, new RuntimeException())
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
        Error err = new Error.Builder(config, new RuntimeException())
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
        Error err = new Error.Builder(config, new RuntimeException())
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
        Error err = new Error.Builder(config, new RuntimeException())
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

        JSONObject errorJson = streamableToJson(error);
        assertEquals(groupingHash, errorJson.get("groupingHash"));
    }

    @Test
    public void testSetSeverity() throws JSONException, IOException {
        error.setSeverity(Severity.INFO);

        JSONObject errorJson = streamableToJson(error);
        assertEquals("info", errorJson.get("severity"));
    }

    private void validateEmptyAttributes(JSONObject severityReason) {
        try {
            severityReason.getJSONObject("attributes");
            fail();
        } catch (JSONException ignored) {
        }
    }
}
