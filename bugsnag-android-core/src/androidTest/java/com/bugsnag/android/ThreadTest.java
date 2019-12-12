package com.bugsnag.android;

import static com.bugsnag.android.BugsnagTestUtils.streamableToJson;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import androidx.test.filters.SmallTest;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import java.io.IOException;
import java.util.Collections;

@SmallTest
public class ThreadTest {

    @Test
    public void testToStreamErrorHandlingThread() throws JSONException, IOException {
        StackTraceElement[] stacktrace = {
            new StackTraceElement("", "run_func", "librunner.so", 5038),
            new StackTraceElement("Runner", "runFunc", "Runner.java", 14),
            new StackTraceElement("App", "launch", "App.java", 70),
        };

        ImmutableConfig config = BugsnagTestUtils.generateImmutableConfig();
        Stacktrace trace = new Stacktrace(stacktrace, Collections.<String>emptyList(),
                NoopLogger.INSTANCE);
        Thread thread = new Thread(24, "main-one", Thread.Type.Android, true, trace);
        JSONObject result = streamableToJson(thread);
        assertEquals(24, result.getLong("id"));
        assertEquals("main-one", result.getString("name"));
        assertEquals("android", result.getString("type"));
        assertTrue(result.getBoolean("errorReportingThread"));

        JSONArray frames = result.getJSONArray("stacktrace");
        assertEquals(3, frames.length());
        assertEquals("run_func", frames.getJSONObject(0).getString("method"));
        assertEquals("librunner.so", frames.getJSONObject(0).getString("file"));
        assertEquals(5038, frames.getJSONObject(0).getInt("lineNumber"));
        assertEquals("Runner.runFunc", frames.getJSONObject(1).getString("method"));
        assertEquals("Runner.java", frames.getJSONObject(1).getString("file"));
        assertEquals(14, frames.getJSONObject(1).getInt("lineNumber"));
        assertEquals("App.launch", frames.getJSONObject(2).getString("method"));
        assertEquals("App.java", frames.getJSONObject(2).getString("file"));
        assertEquals(70, frames.getJSONObject(2).getInt("lineNumber"));
    }

    @Test
    public void testToStreamNonErrorHandlingThread() throws JSONException, IOException {
        StackTraceElement[] stacktrace = {
            new StackTraceElement("", "run_func", "librunner.so", 5038),
            new StackTraceElement("Runner", "runFunc", "Runner.java", 14),
            new StackTraceElement("App", "launch", "App.java", 70),
        };

        ImmutableConfig config = BugsnagTestUtils.generateImmutableConfig();
        Stacktrace trace = new Stacktrace(stacktrace, Collections.<String>emptyList(),
                NoopLogger.INSTANCE);
        Thread thread = new Thread(24, "main-one", Thread.Type.Android,false, trace);
        JSONObject result = streamableToJson(thread);
        assertEquals(24, result.getLong("id"));
        assertEquals("main-one", result.getString("name"));
        assertEquals("android", result.getString("type"));
        assertFalse("Error reporting thread should not be set when false",
                    result.has("errorReportingThread"));

        JSONArray frames = result.getJSONArray("stacktrace");
        assertEquals(3, frames.length());
        assertEquals("run_func", frames.getJSONObject(0).getString("method"));
        assertEquals("librunner.so", frames.getJSONObject(0).getString("file"));
        assertEquals(5038, frames.getJSONObject(0).getInt("lineNumber"));
        assertEquals("Runner.runFunc", frames.getJSONObject(1).getString("method"));
        assertEquals("Runner.java", frames.getJSONObject(1).getString("file"));
        assertEquals(14, frames.getJSONObject(1).getInt("lineNumber"));
        assertEquals("App.launch", frames.getJSONObject(2).getString("method"));
        assertEquals("App.java", frames.getJSONObject(2).getString("file"));
        assertEquals(70, frames.getJSONObject(2).getInt("lineNumber"));
    }
}
