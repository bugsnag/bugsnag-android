package com.bugsnag.android;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;

import javax.annotation.concurrent.NotThreadSafe;

@NotThreadSafe
public class JsonStream extends JsonWriter {

    private final ObjectJsonStreamer objectJsonStreamer;

    public interface Streamable {
        void toStream(@NonNull JsonStream stream) throws IOException;
    }

    private final Writer out;

    /**
     * Constructs a JSONStream
     *
     * @param out the writer
     */
    public JsonStream(Writer out) {
        super(out);
        setSerializeNulls(false);
        this.out = out;
        objectJsonStreamer = new ObjectJsonStreamer();
    }

    // Allow chaining name().value()
    @NonNull
    public JsonStream name(@Nullable String name) throws IOException {
        super.name(name);
        return this;
    }

    /**
     * This gives the Streamable the JsonStream instance and
     * allows lets it write itself into the stream.
     */
    public void value(@Nullable Streamable streamable) throws IOException {
        if (streamable == null) {
            nullValue();
            return;
        }
        streamable.toStream(this);
    }

    /**
     * Serialises an arbitrary object as JSON, handling primitive types as well as
     * Collections, Maps, and arrays.
     */
    public void value(@NonNull Object object) throws IOException {
        objectJsonStreamer.objectToStream(object, this);
    }

    /**
     * Writes a File (its content) into the stream
     */
    public void value(@NonNull File file) throws IOException {
        if (file == null || file.length() <= 0) {
            return;
        }

        super.flush();
        beforeValue(false); // add comma if in array

        // Copy the file contents onto the stream
        Reader input = null;
        try {
            FileInputStream fis = new FileInputStream(file);
            input = new BufferedReader(new InputStreamReader(fis, "UTF-8"));
            IOUtils.copy(input, out);
        } finally {
            IOUtils.closeQuietly(input);
        }

        out.flush();
    }
}
