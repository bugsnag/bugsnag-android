package com.bugsnag.android;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Writer;

class JsonStream {
    static interface Streamable {
        void toStream(JsonStream stream);
    }

    private JsonWriter writer;
    private Writer out;

    JsonStream(Writer out) {
        writer = new JsonWriter(out);
        this.out = out;
    }

    // Wrap JsonWriter methods to swallow exceptions and allow chaining
    JsonStream beginObject() {
        try {
            writer.beginObject();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return this;
    }

    JsonStream endObject() {
        try {
            writer.endObject();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return this;
    }

    JsonStream beginArray() {
        try {
            writer.beginArray();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return this;
    }

    JsonStream endArray() {
        try {
            writer.endArray();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return this;
    }

    JsonStream name(String name) {
        try {
            writer.name(name);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return this;
    }

    JsonStream nullValue() {
        try {
            writer.nullValue();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return this;
    }

    JsonStream value(String val) {
        try {
            writer.value(val);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return this;
    }

    JsonStream value(Number val) {
        try {
            writer.value(val);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return this;
    }

    JsonStream value(Boolean val) {
        try {
            writer.value(val);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return this;
    }

    void close() {
        try {
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Add support for streaming File and Streamable objects
    JsonStream value(Streamable streamable) {
        streamable.toStream(this);
        return this;
    }

    JsonStream value(File file) {
        try {
            writer.flush();

            // Buffer the file contents onto the stream
            FileReader input = new FileReader(file);
            char[] buffer = new char[1024 * 4];
            int n = 0;
            while (-1 != (n = input.read(buffer))) {
                out.write(buffer, 0, n);
            }

            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return this;
    }
}
