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
    public void testUnhandledSerialization() throws Exception {
        Error unhandledErr = new Error.Builder(config, new RuntimeException(""))
            .isUnhandled(true)
            .build();
        JSONObject errorJson = streamableToJson(unhandledErr);
        assertTrue(errorJson.getBoolean("defaultSeverity"));
        assertTrue(errorJson.getBoolean("unhandled"));

        JSONObject severityReason = errorJson.getJSONObject("severityReason");
        assertNotNull(severityReason);
        assertEquals("exception_handler", severityReason.getString("type"));
    }

    @Test
    public void testHandledSerialization() throws Exception {
        Error handledErr = new Error.Builder(config, new RuntimeException("")).build();
        JSONObject errorJson = streamableToJson(handledErr);
        assertTrue(errorJson.getBoolean("defaultSeverity"));
        assertFalse(errorJson.getBoolean("unhandled"));

        try {
            errorJson.getJSONObject("severityReason");
            fail("severityReason should not be present in payload for handled errors");
        } catch (JSONException ignored) {
        }
    }

    @Test
    public void testSeverityMutation() throws Exception {
        Error handledErr = new Error.Builder(config, new RuntimeException("")).build();
        handledErr.setSeverity(Severity.INFO);
        JSONObject errorJson = streamableToJson(handledErr);
        assertFalse(errorJson.getBoolean("defaultSeverity"));
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
}
