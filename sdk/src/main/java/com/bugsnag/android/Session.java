package com.bugsnag.android;

import android.support.annotation.NonNull;

import java.io.IOException;
import java.util.Date;

class Session implements JsonStream.Streamable {

    private final String id;
    private final Date startedAt;
    private final User user;

    public Session(String id, Date startedAt, User user) {
        this.id = id;
        this.startedAt = new Date(startedAt.getTime());
        this.user = user;
    }

    private int unhandledCount;
    private int handledCount;
    private transient boolean autoCaptured;

    String getId() {
        return id;
    }

    Date getStartedAt() {
        return new Date(startedAt.getTime());
    }

    User getUser() {
        return user;
    }

    synchronized int getUnhandledCount() {
        return unhandledCount;
    }

    synchronized int getHandledCount() {
        return handledCount;
    }

    synchronized void incrementHandledErrCount() {
        handledCount++;
    }

    synchronized void incrementUnhandledErrCount() {
        unhandledCount++;
    }

    boolean isAutoCaptured() {
        return autoCaptured;
    }

    void setAutoCaptured(boolean autoCaptured) {
        this.autoCaptured = autoCaptured;
    }

    @Override
    public void toStream(@NonNull JsonStream writer) throws IOException {
        writer.beginObject()
            .name("id").value(id)
            .name("startedAt").value(DateUtils.toISO8601(startedAt));

        if (user != null) {
            writer.name("user").value(user);
        }
        writer.endObject();
    }
}
