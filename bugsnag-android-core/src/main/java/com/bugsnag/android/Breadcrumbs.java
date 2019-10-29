package com.bugsnag.android;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.util.Observable;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;


class Breadcrumbs extends Observable implements JsonStream.Streamable {

    private static final int MAX_PAYLOAD_SIZE = 4096;
    final Queue<Breadcrumb> store = new ConcurrentLinkedQueue<>();

    private final int maxBreadcrumbs;
    private final Logger logger;

    Breadcrumbs(int maxBreadcrumbs, Logger logger) {
        this.logger = logger;
        if (maxBreadcrumbs > 0) {
            this.maxBreadcrumbs = maxBreadcrumbs;
        } else {
            this.maxBreadcrumbs = 0;
        }
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
        try {
            if (breadcrumb.payloadSize() > MAX_PAYLOAD_SIZE) {
                logger.w("Dropping breadcrumb because payload exceeds 4KB limit");
                return;
            }
            store.add(breadcrumb);
            pruneBreadcrumbs();
            setChanged();
            notifyObservers(new NativeInterface.Message(
                        NativeInterface.MessageType.ADD_BREADCRUMB, breadcrumb));
        } catch (IOException ex) {
            logger.w("Dropping breadcrumb because it could not be serialized", ex);
        }
    }

    void clear() {
        store.clear();
        setChanged();
        notifyObservers(new NativeInterface.Message(
                    NativeInterface.MessageType.CLEAR_BREADCRUMBS, null));
    }

    private void pruneBreadcrumbs() {
        // Remove oldest breadcrumbs until new max size reached
        while (store.size() > maxBreadcrumbs) {
            store.poll();
        }
    }
}
