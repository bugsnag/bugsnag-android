package com.bugsnag.android;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Map;

class ObjectJsonStreamer {

    private static final String FILTERED_PLACEHOLDER = "[FILTERED]";
    private static final String OBJECT_PLACEHOLDER = "[OBJECT]";

    String[] filters = {"password"};

    // Write complex/nested values to a JsonStreamer
    void objectToStream(@Nullable Object obj,
                        @NonNull JsonStream writer) throws IOException {
        if (obj == null) {
            writer.nullValue();
        } else if (obj instanceof String) {
            writer.value((String) obj);
        } else if (obj instanceof Number) {
            writer.value((Number) obj);
        } else if (obj instanceof Boolean) {
            writer.value((Boolean) obj);
        } else if (obj instanceof Map) {
            // Map objects
            writer.beginObject();
            for (Object o : ((Map) obj).entrySet()) {

                @SuppressWarnings("unchecked")
                Map.Entry<Object, Object> entry = (Map.Entry<Object, Object>) o;

                Object keyObj = entry.getKey();
                if (keyObj instanceof String) {
                    String key = (String) keyObj;
                    writer.name(key);
                    if (shouldFilter(key)) {
                        writer.value(FILTERED_PLACEHOLDER);
                    } else {
                        objectToStream(entry.getValue(), writer);
                    }
                }
            }
            writer.endObject();
        } else if (obj instanceof Collection) {
            // Collection objects (Lists, Sets etc)
            writer.beginArray();
            for (Object entry : (Collection) obj) {
                objectToStream(entry, writer);
            }
            writer.endArray();
        } else if (obj.getClass().isArray()) {
            // Primitive array objects
            writer.beginArray();
            int length = Array.getLength(obj);
            for (int i = 0; i < length; i += 1) {
                objectToStream(Array.get(obj, i), writer);
            }
            writer.endArray();
        } else {
            writer.value(OBJECT_PLACEHOLDER);
        }
    }

    // Should this key be filtered
    private boolean shouldFilter(@Nullable String key) {
        if (filters == null || key == null) {
            return false;
        }

        for (String filter : filters) {
            if (key.contains(filter)) {
                return true;
            }
        }

        return false;
    }

}
