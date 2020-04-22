package com.bugsnag.android;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.StringWriter;

public class JsonStreamFileTest {

    private StringWriter writer;
    private JsonStream stream;
    private File file;

    /**
     * Deletes a file in the cache directory if it already exists from previous test cases
     *
     */
    @Before
    public void setUp() {
        writer = new StringWriter();
        stream = new JsonStream(writer);
        file = new File("whoops");
        file.delete();
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
