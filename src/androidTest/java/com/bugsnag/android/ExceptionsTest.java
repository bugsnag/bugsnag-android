package com.bugsnag.android;

import android.support.annotation.NonNull;
import java.io.IOException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class ExceptionsTest extends BugsnagTestCase {
    public void testBasicException() throws JSONException, IOException {
        Configuration config = new Configuration("api-key");
        Exceptions exceptions = new Exceptions(config, new RuntimeException("oops"));
        JSONArray exceptionsJson = streamableToJsonArray(exceptions);

        assertEquals(1, exceptionsJson.length());

        JSONObject firstException = (JSONObject)exceptionsJson.get(0);
        assertEquals("java.lang.RuntimeException", firstException.get("errorClass"));
        assertEquals("oops", firstException.get("message"));
        assertNotNull(firstException.get("stacktrace"));
    }

    public void testCauseException() throws JSONException, IOException {
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

    public void testNamedException() throws JSONException, IOException {
        Configuration config = new Configuration("api-key");

        StackTraceElement element = new StackTraceElement("Class", "method", "Class.java", 123);
        StackTraceElement[] frames = new StackTraceElement[] { element };
        Error error = new Error(config, "RuntimeException", "Example message", frames);
        Exceptions exceptions = new Exceptions(config, error.getException());

        JSONObject exceptionJson = streamableToJsonArray(exceptions).getJSONObject(0);
        assertEquals("RuntimeException", exceptionJson.get("errorClass"));
        assertEquals("Example message", exceptionJson.get("message"));

        JSONObject stackframeJson = exceptionJson.getJSONArray("stacktrace").getJSONObject(0);
        assertEquals("Class.method", stackframeJson.get("method"));
        assertEquals("Class.java", stackframeJson.get("file"));
        assertEquals(123, stackframeJson.get("lineNumber"));
    }

    public void testCustomExceptionSerialization() throws JSONException, IOException {
        Configuration config = new Configuration("api-key");
        Exceptions exceptions = new Exceptions(config, new CustomException("Failed serialization"));

        JSONObject exceptionJson = streamableToJsonArray(exceptions).getJSONObject(0);
        assertEquals("CustomizedException", exceptionJson.get("errorClass"));
        assertEquals("Failed serialization", exceptionJson.get("message"));

        JSONObject stackframeJson = exceptionJson.getJSONArray("stacktrace").getJSONObject(0);
        assertEquals("MyFile.run", stackframeJson.get("method"));
        assertEquals("MyFile.java", stackframeJson.get("file"));
        assertEquals(408, stackframeJson.get("lineNumber"));
        assertEquals(18, stackframeJson.get("offset"));
    }
}

class CustomException extends Exception implements JsonStream.Streamable {

    CustomException(String message) {
        super(message);
    }

    public void toStream(@NonNull JsonStream writer) throws IOException {
        writer.beginObject();
            writer.name("errorClass").value("CustomizedException");
            writer.name("message").value(getLocalizedMessage());
            writer.name("stacktrace");
            writer.beginArray();
                writer.beginObject();
                    writer.name("file").value("MyFile.java");
                    writer.name("lineNumber").value(408);
                    writer.name("offset").value(18);
                    writer.name("method").value("MyFile.run");
                writer.endObject();
            writer.endArray();
        writer.endObject();
    }
}
