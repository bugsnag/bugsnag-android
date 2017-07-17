package com.bugsnag.android;

import android.support.test.runner.AndroidJUnit4;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;

@RunWith(AndroidJUnit4.class)
public class StacktraceTest extends BugsnagTestCase {

    @Test
    public void testBasicException() throws JSONException, IOException {
        Configuration config = new Configuration("api-key");
        Throwable exception = new RuntimeException("oops");
        Stacktrace stacktrace = new Stacktrace(config, exception.getStackTrace());
        JSONArray stacktraceJson = streamableToJsonArray(stacktrace);

        JSONObject firstFrame = (JSONObject)stacktraceJson.get(0);
        assertEquals(19, firstFrame.get("lineNumber"));
        assertEquals("com.bugsnag.android.StacktraceTest.testBasicException", firstFrame.get("method"));
        assertEquals("StacktraceTest.java", firstFrame.get("file"));
        assertFalse(firstFrame.has("inProject"));
    }

    @Test
    public void testInProject() throws JSONException, IOException {
        Configuration config = new Configuration("api-key");
        config.setProjectPackages(new String[] {"com.bugsnag.android"});

        Throwable exception = new RuntimeException("oops");
        Stacktrace stacktrace = new Stacktrace(config, exception.getStackTrace());
        JSONArray stacktraceJson = streamableToJsonArray(stacktrace);

        JSONObject firstFrame = (JSONObject)stacktraceJson.get(0);
        assertTrue(firstFrame.getBoolean("inProject"));
    }

}
