package com.bugsnag.android;

import java.io.Writer;

import android.util.JsonWriter;

class JsonStreamer extends JsonWriter {
    static interface Streamable {
        public void toStream(JsonStreamer writer);
    }

    public JsonStreamer(Writer w) {
        super(w);
    }

    public JsonStreamer beginObject() {
        try {
            super.beginObject();
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
        return this;
    }

    public JsonStreamer endObject() {
        try {
            super.beginObject();
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
        return this;
    }

    public JsonStreamer beginArray() {
        try {
            super.beginArray();
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
        return this;
    }

    public JsonStreamer endArray() {
        try {
            super.endArray();
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
        return this;
    }

    public JsonStreamer name(String name) {
        try {
            super.name(name);
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
        return this;
    }

    public JsonStreamer value(double value) {
        try {
            super.value(value);
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
        return this;
    }

    public JsonStreamer value(long value) {
        try {
            super.value(value);
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
        return this;
    }

    public JsonStreamer value(Number value) {
        try {
            super.value(value);
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
        return this;
    }

    public JsonStreamer value(boolean value) {
        try {
            super.value(value);
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
        return this;
    }

    public JsonStreamer value(String value) {
        try {
            super.value(value);
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
        return this;
    }

    public JsonStreamer value(Streamable streamable) {
        streamable.toStream(this);
        return this;
    }
}
