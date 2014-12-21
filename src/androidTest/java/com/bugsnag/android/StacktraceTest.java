package com.bugsnag.android;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class StacktraceTest extends BugsnagTestCase {
    public void testBasicException() throws JSONException {
        Configuration config = new Configuration("api-key");
        Throwable exception = new RuntimeException("oops");
        Stacktrace stacktrace = new Stacktrace(config, exception.getStackTrace());
        JSONArray stacktraceJson = streamableToJsonArray(stacktrace);

        JSONObject firstFrame = (JSONObject)stacktraceJson.get(0);
        assertEquals(10, firstFrame.get("lineNumber"));
        assertEquals("com.bugsnag.android.StacktraceTest.testBasicException", firstFrame.get("method"));
        assertEquals("StacktraceTest.java", firstFrame.get("file"));
        assertFalse(firstFrame.has("inProject"));
    }

    public void testInProject() throws JSONException {
        Configuration config = new Configuration("api-key");
        config.projectPackages = new String[] {"com.bugsnag.android"};

        Throwable exception = new RuntimeException("oops");
        Stacktrace stacktrace = new Stacktrace(config, exception.getStackTrace());
        JSONArray stacktraceJson = streamableToJsonArray(stacktrace);

        JSONObject firstFrame = (JSONObject)stacktraceJson.get(0);
        assertTrue(firstFrame.getBoolean("inProject"));
    }
}
