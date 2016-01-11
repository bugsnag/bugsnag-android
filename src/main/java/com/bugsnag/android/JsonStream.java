package com.bugsnag.android;

import android.support.annotation.NonNull;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Writer;

class JsonStream extends JsonWriter {
    interface Streamable {
        void toStream(@NonNull JsonStream stream) throws IOException;
    }

    private final Writer out;

    JsonStream(Writer out) {
        super(out);
        this.out = out;
    }

    // Allow chaining name().value()
    public JsonStream name(@NonNull String name) throws IOException {
        super.name(name);
        return this;
    }

    /**
     * Writes a Boolean value into the stream if it is not null,
     * otherwise a null value.
     */
    public void value(@NonNull Boolean value) throws IOException {
        //noinspection ConstantConditions
        if (value == null) {
            nullValue();
            return;
        }
        super.value(value);
    }

    /**
     * Writes a String value into the stream if it is not null,
     * otherwise a null value.
     */
    @Override
    public JsonWriter value(@NonNull String value) throws IOException {
        //noinspection ConstantConditions
        if (value == null)
            return nullValue();
        return super.value(value);
    }

    /**
     * Writes a Number value into the stream if it is not null,
     * otherwise a null value.
     */
    @Override
    public JsonWriter value(@NonNull Number value) throws IOException {
        //noinspection ConstantConditions
        if (value == null)
            return nullValue();
        return super.value(value);
    }

    /**
     * This gives the Streamable the JsonStream instance and
     * allows lets it write itself into the stream.
     */
    public void value(@NonNull Streamable streamable) throws IOException {
        streamable.toStream(this);
    }

    /**
     * Writes a File (its content) into the stream
     */
    public void value(@NonNull File file) throws IOException {
        super.flush();

        // Copy the file contents onto the stream
        FileReader input = null;
        try {
            input = new FileReader(file);
            IOUtils.copy(input, out);
        } finally {
            IOUtils.closeQuietly(input);
        }

        out.flush();
    }
}
