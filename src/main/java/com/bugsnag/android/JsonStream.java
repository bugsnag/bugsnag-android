package com.bugsnag.android;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Writer;

class JsonStream extends JsonWriter {
    static interface Streamable {
        void toStream(JsonStream stream) throws IOException;
    }

    private Writer out;

    public JsonStream(Writer out) {
        super(out);
        this.out = out;
    }

    // Allow chaining name().value()
    public JsonStream name(String name) throws IOException {
        super.name(name);
        return this;
    }

    // Add support for Streamable values
    public void value(Streamable streamable) throws IOException {
        streamable.toStream(this);
    }

    // Add support for File values
    public void value(File file) throws IOException {
        super.flush();

        // Buffer the file contents onto the stream
        FileReader input = new FileReader(file);
        char[] buffer = new char[1024 * 4];
        int n = 0;
        while (-1 != (n = input.read(buffer))) {
            out.write(buffer, 0, n);
        }

        out.flush();
    }
}
