package com.bugsnag.android;

import java.io.IOException;
import java.io.StringWriter;

import org.json.JSONException;
import org.json.JSONObject;

public class JsonStreamTest extends BugsnagTestCase {
    public void testSaneValues() throws JSONException, IOException {
        StringWriter writer = new StringWriter();
        JsonStream stream = new JsonStream(writer);

        Long nullLong = null;
        Boolean nullBoolean = null;
        String nullString = null;
        Integer nullInteger = null;
        Float nullFloat = null;
        Double nullDouble = null;

        stream.beginObject();
        stream.name("nullLong").value(nullLong);
        stream.name("nullBoolean").value(nullBoolean);
        stream.name("nullString").value(nullString);
        stream.name("nullInteger").value(nullInteger);
        stream.name("nullFloat").value(nullFloat);
        stream.name("nullDouble").value(nullDouble);
        stream.name("string").value("string");
        stream.name("int").value(123);
        stream.name("long").value(123l);
        stream.name("float").value(123.45f);
        stream.endObject();

        JSONObject json = new JSONObject(writer.toString());
        assertTrue(json.isNull("nullLong"));
        assertTrue(json.isNull("nullBoolean"));
        assertTrue(json.isNull("nullString"));
        assertTrue(json.isNull("nullInteger"));
        assertTrue(json.isNull("nullFloat"));
        assertTrue(json.isNull("nullDouble"));
        assertEquals("string", json.getString("string"));
        assertEquals(123, json.getInt("int"));
        assertEquals(123l, json.getLong("long"));
    }
}
