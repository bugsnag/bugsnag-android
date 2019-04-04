package com.bugsnag.android;

import static com.bugsnag.android.BugsnagTestUtils.streamableToJsonArray;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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
import java.util.ArrayList;
import java.util.List;

@FlakyTest(detail = "Checks a stacktrace's line number, so fails when lines are added/deleted.")
@RunWith(AndroidJUnit4.class)
@SmallTest
public class StacktraceTest {

    private Configuration config;
    private Throwable exception;

    /**
     * Creates an initial exception
     */
    @Before
    public void setUp() throws Exception {
        config = new Configuration("api-key");
        exception = new RuntimeException("oops");
    }

    @Test
    public void testBasicException() throws JSONException, IOException {
        String[] projectPackages = config.getProjectPackages();
        Stacktrace stacktrace = new Stacktrace(exception.getStackTrace(), projectPackages);
        JSONArray stacktraceJson = streamableToJsonArray(stacktrace);

        JSONObject firstFrame = (JSONObject) stacktraceJson.get(0);
        assertEquals(37, firstFrame.get("lineNumber"));
        assertEquals("com.bugsnag.android.StacktraceTest.setUp", firstFrame.get("method"));
        assertEquals("StacktraceTest.java", firstFrame.get("file"));
        assertFalse(firstFrame.has("inProject"));
    }

    @Test
    public void testInProject() throws JSONException, IOException {
        config.setProjectPackages(new String[]{"com.bugsnag.android"});

        String[] projectPackages = config.getProjectPackages();
        Stacktrace stacktrace = new Stacktrace(exception.getStackTrace(), projectPackages);
        JSONArray stacktraceJson = streamableToJsonArray(stacktrace);

        JSONObject firstFrame = (JSONObject) stacktraceJson.get(0);
        assertTrue(firstFrame.getBoolean("inProject"));
    }

    @Test
    public void testStacktraceTrimming() throws Throwable {
        List<StackTraceElement> elements = new ArrayList<>();

        for (int k = 0; k < 1000; k++) {
            elements.add(new StackTraceElement("SomeClass",
                "someMethod", "someFile", k));
        }

        StackTraceElement[] ary = new StackTraceElement[elements.size()];
        Stacktrace stacktrace = new Stacktrace(elements.toArray(ary), config.getProjectPackages());
        JSONArray jsonArray = streamableToJsonArray(stacktrace);
        assertEquals(200, jsonArray.length());
    }

    @Test
    public void testClassNameResolution() throws JSONException, IOException {
        StackTraceElement[] stackTraceElements = {
            new StackTraceElement("SomeClass", "someMethod", "someFile", 12)};
        Stacktrace stacktrace = new Stacktrace(stackTraceElements, config.getProjectPackages());
        JSONArray stacktraceJson = streamableToJsonArray(stacktrace);

        JSONObject frame = (JSONObject) stacktraceJson.get(0);
        assertEquals("SomeClass.someMethod", frame.get("method"));

        StackTraceElement stackTraceElement = new StackTraceElement("",
            "someMethod", "someFile", 12);
        Stacktrace stacktrace1 = new Stacktrace(
            new StackTraceElement[]{stackTraceElement}, config.getProjectPackages());
        stacktraceJson = streamableToJsonArray(stacktrace1);

        frame = (JSONObject) stacktraceJson.get(0);
        assertEquals("someMethod", frame.get("method"));
    }
}
