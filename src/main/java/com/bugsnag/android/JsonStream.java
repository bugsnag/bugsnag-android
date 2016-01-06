package com.bugsnag.android;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Writer;

class JsonStream extends JsonWriter {
    interface Streamable {
        void toStream(JsonStream stream) throws IOException;
    }

    private Writer out;

    JsonStream(Writer out) {
        super(out);
        this.out = out;
    }

    // Allow chaining name().value()
    public JsonStream name(String name) throws IOException {
        super.name(name);
        return this;
    }

    // Add null-protection
    void value(Boolean value) throws IOException {
        if (value == null) {
            nullValue();
        } else {
            super.value(value);
        }
    }

    // Add support for Streamable values
    void value(Streamable streamable) throws IOException {
        streamable.toStream(this);
    }

    // Add support for File values
    void value(File file) throws IOException {
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
