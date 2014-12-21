package com.bugsnag.android;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class ExceptionChainTest extends BugsnagTestCase {
    public void testBasicException() throws JSONException {
        Configuration config = new Configuration("api-key");
        ExceptionChain exceptions = new ExceptionChain(config, new RuntimeException("oops"));
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
        ExceptionChain exceptions = new ExceptionChain(config, ex);
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
}
