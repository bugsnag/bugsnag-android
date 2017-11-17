package com.bugsnag.android;

import android.support.annotation.NonNull;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;


class Breadcrumbs implements JsonStream.Streamable {
    private static class Breadcrumb implements JsonStream.Streamable {
        private static final int MAX_MESSAGE_LENGTH = 140;
        private static final String DEFAULT_NAME = "manual";
        private static final String MESSAGE_METAKEY = "message";
        private final String TIMESTAMP_KEY = "timestamp";
        private final String NAME_KEY = "name";
        private final String METADATA_KEY = "metaData";
        private final String TYPE_KEY = "type";
        final String timestamp;
        @NonNull
        final String name;
        @NonNull
        final BreadcrumbType type;
        @NonNull
        final Map<String, String> metadata;

        Breadcrumb(@NonNull String message) {
            this.timestamp = DateUtils.toISO8601(new Date());
            this.type = BreadcrumbType.MANUAL;
            this.metadata = Collections.singletonMap(MESSAGE_METAKEY, message.substring(0, Math.min(message.length(), MAX_MESSAGE_LENGTH)));
            this.name = DEFAULT_NAME;
        }

        Breadcrumb(@NonNull String name, @NonNull BreadcrumbType type, @NonNull Map<String, String> metadata) {
            this.timestamp = DateUtils.toISO8601(new Date());
            this.type = type;
            this.metadata = metadata;
            this.name = name;
        }

        @Override
        public void toStream(@NonNull JsonStream writer) throws IOException {
            writer.beginObject();
            writer.name(TIMESTAMP_KEY).value(this.timestamp);
            writer.name(NAME_KEY).value(this.name);
            writer.name(TYPE_KEY).value(this.type.toString());
            writer.name(METADATA_KEY);
            writer.beginObject();

            // sort metadata alphabetically
            List<String> keys = new ArrayList<>(metadata.keySet());
            Collections.sort(keys, String.CASE_INSENSITIVE_ORDER);

            for (String key : keys) {
                writer.name(key).value(metadata.get(key));
            }

            writer.endObject();
            writer.endObject();
        }

        public int payloadSize() throws IOException {
            StringWriter writer = new StringWriter();
            JsonStream jsonStream = new JsonStream(writer);
            toStream(jsonStream);

            return writer.toString().length();
        }
    }

    private static final int DEFAULT_MAX_SIZE = 20;
    private static final int MAX_PAYLOAD_SIZE = 4096;
    final Queue<Breadcrumb> store = new ConcurrentLinkedQueue<>();
    private int maxSize = DEFAULT_MAX_SIZE;

    @Override
    public void toStream(@NonNull JsonStream writer) throws IOException {
        writer.beginArray();

        for (Breadcrumb breadcrumb : store) {
            breadcrumb.toStream(writer);
        }

        writer.endArray();
    }

    void add(@NonNull String message) {
        addToStore(new Breadcrumb(message));
    }

    void add(@NonNull String name, @NonNull BreadcrumbType type, @NonNull Map<String, String> metadata) {
        addToStore(new Breadcrumb(name, type, metadata));
    }

    void clear() {
        store.clear();
    }

    void setSize(int size) {
        if (size < 0) {
            Logger.warn("Ignoring invalid breadcrumb capacity. Must be >= 0.");
            return;
        }

        this.maxSize = size;
        // Remove oldest breadcrumbs until reaching the required size
        while (store.size() > size) {
            store.poll();
        }
    }

    private void addToStore(@NonNull Breadcrumb breadcrumb) {
        try {
            if (breadcrumb.payloadSize() > MAX_PAYLOAD_SIZE) {
                Logger.warn("Dropping breadcrumb because payload exceeds 4KB limit");
                return;
            }
            store.add(breadcrumb);
            if (store.size() > maxSize) {
                // Remove oldest breadcrumb
                store.poll();
            }
        } catch (IOException ex) {
            Logger.warn("Dropping breadcrumb because it could not be serialized", ex);
        }
    }
}
