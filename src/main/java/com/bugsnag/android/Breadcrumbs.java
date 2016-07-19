package com.bugsnag.android;

import android.support.annotation.NonNull;

import java.io.IOException;
import java.util.Date;
import java.util.Queue;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;


class Breadcrumbs implements JsonStream.Streamable {
    private static class Breadcrumb {
        private static final int MAX_MESSAGE_LENGTH = 140;
        private static final String DEFAULT_NAME = "manual";
        private static final String MESSAGE_METAKEY = "message";
        final String timestamp;
        final String name;
        final BreadcrumbType type;
        final HashMap<String, String> metadata;

        Breadcrumb(@NonNull String message) {
            this.timestamp = DateUtils.toISO8601(new Date());
            this.type = BreadcrumbType.MANUAL;
            HashMap<String, String> metadata = new HashMap<String, String>();
            metadata.put(MESSAGE_METAKEY,
                    message.substring(0, Math.min(message.length(), MAX_MESSAGE_LENGTH)));
            this.metadata = metadata;
            this.name = DEFAULT_NAME;
        }

        Breadcrumb(@NonNull String name, BreadcrumbType type, HashMap<String, String> metadata) {
            this.timestamp = DateUtils.toISO8601(new Date());
            this.type = type;
            this.metadata = metadata;
            this.name = name;
        }
    }

    private static final int DEFAULT_MAX_SIZE = 20;
    private final Queue<Breadcrumb> store = new ConcurrentLinkedQueue<>();
    private int maxSize = DEFAULT_MAX_SIZE;
    private final String TIMESTAMP_KEY = "timestamp";
    private final String NAME_KEY = "name";
    private final String METADATA_KEY = "metadata";
    private final String TYPE_KEY = "type";

    public void toStream(@NonNull JsonStream writer) throws IOException {
        writer.beginArray();

        for (Breadcrumb breadcrumb : store) {
            writer.beginObject();
            writer.name(TIMESTAMP_KEY);
            writer.value(breadcrumb.timestamp);
            writer.name(NAME_KEY);
            writer.value(breadcrumb.name);
            writer.name(TYPE_KEY);
            writer.value(breadcrumb.type.serialize());
            writer.name(METADATA_KEY);
            writer.beginObject();
            for (Map.Entry<String, String> entry : breadcrumb.metadata.entrySet()) {
                writer.name(entry.getKey());
                writer.value(entry.getValue());
            }
            writer.endObject();
            writer.endObject();
        }

        writer.endArray();
    }

    void add(@NonNull String message) {
        addToStore(new Breadcrumb(message));
    }

    void add(@NonNull String name, BreadcrumbType type, HashMap<String, String> metadata) {
        addToStore(new Breadcrumb(name, type, metadata));
    }

    void clear() {
        store.clear();
    }

    void setSize(int size) {
        if (size > store.size()) {
            this.maxSize = size;
        } else {
            // Remove oldest breadcrumbs until reaching the required size
            while (store.size() > size) {
                store.poll();
            }
        }
    }

    private void addToStore(Breadcrumb breadcrumb) {
        if (store.size() >= maxSize) {
            // Remove oldest breadcrumb
            store.poll();
        }
        store.add(breadcrumb);
    }
}
