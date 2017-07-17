package com.bugsnag.android;

import android.support.test.runner.AndroidJUnit4;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;

@RunWith(AndroidJUnit4.class)
public class ErrorTest extends BugsnagTestCase {

    @Test
    public void testShouldIgnoreClass() {
        Configuration config = new Configuration("api-key");
        config.setIgnoreClasses(new String[] {"java.io.IOException"});

        // Shouldn't ignore classes not in ignoreClasses
        Error error = new Error(config, new RuntimeException("Test"));
        assertFalse(error.shouldIgnoreClass());

        // Should ignore errors in ignoreClasses
        error = new Error(config, new java.io.IOException("Test"));
        assertTrue(error.shouldIgnoreClass());
    }

    @Test
    public void testGetExceptionName() {
        Configuration config = new Configuration("api-key");

        Error error = new Error(config, new RuntimeException("Test"));
        assertEquals("java.lang.RuntimeException", error.getExceptionName());
    }

    @Test
    public void testGetExceptionMessage() {
        Configuration config = new Configuration("api-key");

        Error error = new Error(config, new RuntimeException("Example message"));
        assertEquals("Example message", error.getExceptionMessage());
    }

    @Test
    public void testBasicSerialization() throws JSONException, IOException {
        Configuration config = new Configuration("api-key");
        Error error = new Error(config, new RuntimeException("Example message"));

        JSONObject errorJson = streamableToJson(error);
        assertEquals("warning", errorJson.get("severity"));
        assertEquals("3", errorJson.get("payloadVersion"));
        assertNotNull(errorJson.get("severity"));
        assertNotNull(errorJson.get("metaData"));
        assertNotNull(errorJson.get("threads"));
    }

    @Test
    public void testSetContext() throws JSONException, IOException {
        Configuration config = new Configuration("api-key");
        Error error = new Error(config, new RuntimeException("Example message"));
        error.setContext("ExampleContext");

        JSONObject errorJson = streamableToJson(error);
        assertEquals("ExampleContext", errorJson.get("context"));
    }

    @Test
    public void testSetGroupingHash() throws JSONException, IOException {
        Configuration config = new Configuration("api-key");
        Error error = new Error(config, new RuntimeException("Example message"));
        error.setGroupingHash("herpderp");

        JSONObject errorJson = streamableToJson(error);
        assertEquals("herpderp", errorJson.get("groupingHash"));
    }

    @Test
    public void testSetSeverity() throws JSONException, IOException {
        Configuration config = new Configuration("api-key");
        Error error = new Error(config, new RuntimeException("Example message"));
        error.setSeverity(Severity.INFO);

        JSONObject errorJson = streamableToJson(error);
        assertEquals("info", errorJson.get("severity"));
    }
}
