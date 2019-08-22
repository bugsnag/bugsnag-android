package com.bugsnag.android;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

public class JsonWriterBugsnagTest {

    @Test(expected = IOException.class)
    public void testClose() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        OutputStreamWriter writer = new OutputStreamWriter(baos);
        JsonWriter jsonWriter = new JsonWriter(writer);

        jsonWriter.beginObject().endObject();
        jsonWriter.flush();
        assertEquals("{}", new String(baos.toByteArray(), "UTF-8"));

        jsonWriter.close();
        writer.write(5); // can't write to a closed stream, throws IOException
    }

}
