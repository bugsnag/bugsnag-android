package com.bugsnag.android;

import org.json.JSONException;
import org.json.JSONObject;

public class ErrorTest extends BugsnagTestCase {
    public void testShouldIgnoreClass() {
        Configuration config = new Configuration("api-key");
        config.ignoreClasses = new String[] {"java.io.IOException"};

        // Shouldn't ignore classes not in ignoreClasses
        Error error = new Error(config, new RuntimeException("Test"));
        assertFalse(error.shouldIgnoreClass());

        // Should ignore errors in ignoreClasses
        error = new Error(config, new java.io.IOException("Test"));
        assertTrue(error.shouldIgnoreClass());
    }

    public void testGetExceptionName() {
        Configuration config = new Configuration("api-key");

        Error error = new Error(config, new RuntimeException("Test"));
        assertEquals("java.lang.RuntimeException", error.getExceptionName());
    }

    public void testGetExceptionMessage() {
        Configuration config = new Configuration("api-key");

        Error error = new Error(config, new RuntimeException("Example message"));
        assertEquals("Example message", error.getExceptionMessage());
    }

    public void testBasicSerialization() throws JSONException {
        Configuration config = new Configuration("api-key");
        Error error = new Error(config, new RuntimeException("Example message"));

        JSONObject errorJson = streamableToJson(error);
        assertEquals("warning", errorJson.get("severity"));
        assertEquals("2", errorJson.get("payloadVersion"));
        assertNotNull(errorJson.get("severity"));
        assertNotNull(errorJson.get("metaData"));
        assertNotNull(errorJson.get("threads"));
    }

    public void testSetContext() throws JSONException {
        Configuration config = new Configuration("api-key");
        Error error = new Error(config, new RuntimeException("Example message"));
        error.setContext("ExampleContext");

        JSONObject errorJson = streamableToJson(error);
        assertEquals("ExampleContext", errorJson.get("context"));
    }

    public void testSetGroupingHash() throws JSONException {
        Configuration config = new Configuration("api-key");
        Error error = new Error(config, new RuntimeException("Example message"));
        error.setGroupingHash("herpderp");

        JSONObject errorJson = streamableToJson(error);
        assertEquals("herpderp", errorJson.get("groupingHash"));
    }

    public void testSetSeverity() throws JSONException {
        Configuration config = new Configuration("api-key");
        Error error = new Error(config, new RuntimeException("Example message"));
        error.setSeverity(Severity.INFO);

        JSONObject errorJson = streamableToJson(error);
        assertEquals("info", errorJson.get("severity"));
    }
}
