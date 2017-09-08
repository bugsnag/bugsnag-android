package com.bugsnag.android;

import android.support.test.filters.FlakyTest;
import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;

import static com.bugsnag.android.BugsnagTestUtils.streamableToJsonArray;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@FlakyTest(detail = "Checks a stacktrace's line number, so fails when lines are added/deleted.")
@RunWith(AndroidJUnit4.class)
@SmallTest
public class StacktraceTest {

    private Configuration config;
    private Throwable exception;

    @Before
    public void setUp() throws Exception {
        config = new Configuration("api-key");
        exception = new RuntimeException("oops");
    }

    @Test
    public void testBasicException() throws JSONException, IOException {
        Stacktrace stacktrace = new Stacktrace(config, exception.getStackTrace());
        JSONArray stacktraceJson = streamableToJsonArray(stacktrace);

        JSONObject firstFrame = (JSONObject) stacktraceJson.get(0);
        assertEquals(32, firstFrame.get("lineNumber"));
        assertEquals("com.bugsnag.android.StacktraceTest.setUp", firstFrame.get("method"));
        assertEquals("StacktraceTest.java", firstFrame.get("file"));
        assertFalse(firstFrame.has("inProject"));
    }

    @Test
    public void testInProject() throws JSONException, IOException {
        config.setProjectPackages(new String[]{"com.bugsnag.android"});

        Stacktrace stacktrace = new Stacktrace(config, exception.getStackTrace());
        JSONArray stacktraceJson = streamableToJsonArray(stacktrace);

        JSONObject firstFrame = (JSONObject) stacktraceJson.get(0);
        assertTrue(firstFrame.getBoolean("inProject"));
    }

}
