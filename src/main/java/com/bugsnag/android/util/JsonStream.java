package com.bugsnag.android;

import java.io.File;
import java.io.IOException;
import java.io.Writer;

import android.util.JsonWriter;

class JsonStream {
    static interface Streamable {
        public void toStream(JsonStream stream);
    }

    private JsonWriter writer;
    private Writer out;

    public JsonStream(Writer w) {
        out = w;
        writer = new JsonWriter(w);
    }

    public JsonStream object() {
        try {
            writer.beginObject();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return this;
    }

    public JsonStream endObject() {
        try {
            writer.endObject();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return this;
    }

    public JsonStream array() {
        try {
            writer.beginArray();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return this;
    }

    public JsonStream endArray() {
        try {
            writer.endArray();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return this;
    }

    public JsonStream name(String name) {
        try {
            writer.name(name);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return this;
    }

    public JsonStream value(double value) {
        try {
            writer.value(value);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return this;
    }

    public JsonStream value(long value) {
        try {
            writer.value(value);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return this;
    }

    public JsonStream value(Number value) {
        try {
            writer.value(value);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return this;
    }

    public JsonStream value(boolean value) {
        try {
            writer.value(value);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return this;
    }

    public JsonStream value(String value) {
        try {
            writer.value(value);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return this;
    }

    public JsonStream value(Streamable streamable) {
        streamable.toStream(this);
        return this;
    }

    public JsonStream value(File file) {
        try {
            writer.flush();
            IOUtils.copy(file, out);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return this;
    }

    public void close() {
        IOUtils.close(writer);
    }
}
