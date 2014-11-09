package com.bugsnag.android;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.Map;
import java.util.List;

import android.util.JsonWriter;

class JsonStream {
    static interface Streamable {
        void toStream(JsonStream stream);
    }

    private JsonWriter writer;
    private Writer out;

    JsonStream(Writer w) {
        out = w;
        writer = new JsonWriter(w);
    }

    JsonStream object() {
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

    JsonStream array() {
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

    JsonStream value(Streamable streamable) {
        streamable.toStream(this);
        return this;
    }

    JsonStream value(File file) {
        try {
            writer.flush();
            IOUtils.copy(file, out);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return this;
    }

    JsonStream value(Map<String, Object> map) {
        object();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            name(entry.getKey());
            value(entry.getValue());
        }
        endObject();

        return this;
    }

    JsonStream value(List list) {
        array();
        for(Object el : list) {
            value(el);
        }
        endArray();

        return this;
    }

    JsonStream value(Object val) {
        try {
            if(val == null) {
                writer.nullValue();
            } else if(val instanceof String) {
                writer.value((String)val);
            } else if(val instanceof Number) {
                writer.value((Number)val);
            } else if(val instanceof Boolean) {
                writer.value((Boolean)val);
            } else if(val instanceof Map) {
                value((Map)val);
            } else if(val instanceof List) {
                value((List)val);
            } else {
                writer.value("[object]");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return this;
    }

    void close() {
        IOUtils.close(writer);
    }
}
