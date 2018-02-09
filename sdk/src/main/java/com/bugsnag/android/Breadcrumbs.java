package com.bugsnag.android;

import android.support.annotation.NonNull;

import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;


class Breadcrumbs implements JsonStream.Streamable {

    private static final int DEFAULT_MAX_SIZE = 32;
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

    void add(@NonNull Breadcrumb breadcrumb) {
        addToStore(breadcrumb);
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
