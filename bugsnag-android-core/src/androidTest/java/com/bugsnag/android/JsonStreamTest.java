package com.bugsnag.android;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.SmallTest;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;

@SmallTest
public class JsonStreamTest {

    private StringWriter writer;
    private JsonStream stream;
    private File file;

    /**
     * Deletes a file in the cache directory if it already exists from previous test cases
     *
     * @throws Exception if setup failed
     */
    @Before
    public void setUp() throws Exception {
        writer = new StringWriter();
        stream = new JsonStream(writer);
        File cacheDir = ApplicationProvider.getApplicationContext().getCacheDir();
        file = new File(cacheDir, "whoops");
        file.delete();
    }

    @Test
    public void testSaneValues() throws JSONException, IOException {
        final Long nullLong = null;
        final Boolean nullBoolean = null;
        final String nullString = null;
        final Integer nullInteger = null;
        final Float nullFloat = null;
        final Double nullDouble = null;

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

    @Test
    public void testEmptyFileValue() throws Throwable {
        file.createNewFile();
        stream.beginArray();
        stream.value(file);
        stream.value(file);
        stream.endArray();
        assertEquals("[]", writer.toString());
    }

    @Test
    public void testNullFileValue() throws Throwable {
        File file = null;
        stream.beginArray();
        stream.value(file);
        stream.value(file);
        stream.endArray();
        assertEquals("[]", writer.toString());
    }

    @Test
    public void testDeletedFile() throws Throwable {
        file.createNewFile();
        file.delete();
        stream.beginArray();
        stream.value(file);
        stream.value(file);
        stream.endArray();
        assertEquals("[]", writer.toString());
    }

}
