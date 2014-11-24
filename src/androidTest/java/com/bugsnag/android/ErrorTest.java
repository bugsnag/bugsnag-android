package com.bugsnag.android;

import org.json.JSONException;
import org.json.JSONObject;

import com.bugsnag.android.Configuration;
import com.bugsnag.android.Error;

public class ErrorTest extends BugsnagTestCase {
    public void testShouldIgnore() {
        Configuration config = new Configuration("api-key");
        config.ignoreClasses = new String[] {"java.io.IOException"};

        // Shouldn't ignore classes not in ignoreClasses
        Error error = new Error(config, new RuntimeException("Test"));
        assertFalse(error.shouldIgnore());

        // Should ignore errors in ignoreClasses
        error = new Error(config, new java.io.IOException("Test"));
        assertTrue(error.shouldIgnore());
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

    public void testToStream() throws JSONException {
        Configuration config = new Configuration("api-key");
        Error error = new Error(config, new RuntimeException("Example message"));

        JSONObject errorJson = streamableToJson(error);
        assertEquals("warning", errorJson.get("severity"));
        // TODO: Test all other fields are correct
    }
}
