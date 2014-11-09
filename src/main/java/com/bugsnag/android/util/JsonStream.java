package com.bugsnag.android;

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
        writer.setIndent("  ");
    }

    public JsonStream object() {
        try {
            writer.beginObject();
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
        return this;
    }

    public JsonStream endObject() {
        try {
            writer.endObject();
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
        return this;
    }

    public JsonStream array() {
        try {
            writer.beginArray();
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
        return this;
    }

    public JsonStream endArray() {
        try {
            writer.endArray();
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
        return this;
    }

    public JsonStream name(String name) {
        try {
            writer.name(name);
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
        return this;
    }

    public JsonStream value(double value) {
        try {
            writer.value(value);
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
        return this;
    }

    public JsonStream value(long value) {
        try {
            writer.value(value);
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
        return this;
    }

    public JsonStream value(Number value) {
        try {
            writer.value(value);
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
        return this;
    }

    public JsonStream value(boolean value) {
        try {
            writer.value(value);
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
        return this;
    }

    public JsonStream value(String value) {
        try {
            writer.value(value);
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
        return this;
    }

    public JsonStream value(Streamable streamable) {
        streamable.toStream(this);
        return this;
    }

    public void close() {
        try {
            writer.close();
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }
}
