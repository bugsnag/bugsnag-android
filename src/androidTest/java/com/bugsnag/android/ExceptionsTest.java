package com.bugsnag.android;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class ExceptionsTest extends BugsnagTestCase {
    public void testBasicException() throws JSONException {
        Configuration config = new Configuration("api-key");
        Exceptions exceptions = new Exceptions(config, new RuntimeException("oops"));
        JSONArray exceptionsJson = streamableToJsonArray(exceptions);

        assertEquals(1, exceptionsJson.length());

        JSONObject firstException = (JSONObject)exceptionsJson.get(0);
        assertEquals("java.lang.RuntimeException", firstException.get("errorClass"));
        assertEquals("oops", firstException.get("message"));
        assertNotNull(firstException.get("stacktrace"));
    }

    public void testCauseException() throws JSONException {
        Configuration config = new Configuration("api-key");
        Throwable ex = new RuntimeException("oops", new Exception("cause"));
        Exceptions exceptions = new Exceptions(config, ex);
        JSONArray exceptionsJson = streamableToJsonArray(exceptions);

        assertEquals(2, exceptionsJson.length());

        JSONObject firstException = (JSONObject)exceptionsJson.get(0);
        assertEquals("java.lang.RuntimeException", firstException.get("errorClass"));
        assertEquals("oops", firstException.get("message"));
        assertNotNull(firstException.get("stacktrace"));

        JSONObject causeException = (JSONObject)exceptionsJson.get(1);
        assertEquals("java.lang.Exception", causeException.get("errorClass"));
        assertEquals("cause", causeException.get("message"));
        assertNotNull(causeException.get("stacktrace"));
    }

    public void testNamedException() throws JSONException {
        Configuration config = new Configuration("api-key");

        StackTraceElement element = new StackTraceElement("Class", "method", "Class.java", 123);
        StackTraceElement[] frames = new StackTraceElement[] { element };
        Exceptions exceptions = new Exceptions(config, "RuntimeException", "Example message", frames);

        JSONObject exceptionJson = streamableToJsonArray(exceptions).getJSONObject(0);
        assertEquals("RuntimeException", exceptionJson.get("errorClass"));
        assertEquals("Example message", exceptionJson.get("message"));

        JSONObject stackframeJson = exceptionJson.getJSONArray("stacktrace").getJSONObject(0);
        assertEquals("Class.method", stackframeJson.get("method"));
        assertEquals("Class.java", stackframeJson.get("file"));
        assertEquals(123, stackframeJson.get("lineNumber"));
    }
}
