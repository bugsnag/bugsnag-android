package com.bugsnag.android;

import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.io.StringWriter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
@SmallTest
public class JsonStreamTest {

    private StringWriter writer;
    private JsonStream stream;

    @Before
    public void setUp() throws Exception {
        writer = new StringWriter();
        stream = new JsonStream(writer);
    }

    @Test
    public void testSaneValues() throws JSONException, IOException {
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
        stream.name("long").value(123L);
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
        assertEquals(123L, json.getLong("long"));
    }
}
