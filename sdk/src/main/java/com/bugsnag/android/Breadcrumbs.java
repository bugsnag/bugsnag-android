package com.bugsnag.android;

import android.support.annotation.NonNull;

import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;


class Breadcrumbs implements JsonStream.Streamable {

    private static final int MAX_PAYLOAD_SIZE = 4096;
    final Queue<Breadcrumb> store = new ConcurrentLinkedQueue<>();

    private final Configuration configuration;

    Breadcrumbs(Configuration configuration) {
        this.configuration = configuration;
    }

    @Override
    public void toStream(@NonNull JsonStream writer) throws IOException {
        pruneBreadcrumbs();
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

    private void addToStore(@NonNull Breadcrumb breadcrumb) {
        try {
            if (breadcrumb.payloadSize() > MAX_PAYLOAD_SIZE) {
                Logger.warn("Dropping breadcrumb because payload exceeds 4KB limit");
                return;
            }
            store.add(breadcrumb);
            pruneBreadcrumbs();
        } catch (IOException ex) {
            Logger.warn("Dropping breadcrumb because it could not be serialized", ex);
        }
    }

    private void pruneBreadcrumbs() {
        int maxBreadcrumbs = configuration.getMaxBreadcrumbs();

        // Remove oldest breadcrumb
        while (store.size() > maxBreadcrumbs) {
            store.poll();
        }
    }
}
