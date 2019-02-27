package com.bugsnag.android;

import android.support.annotation.NonNull;

import java.io.IOException;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

class Session implements JsonStream.Streamable {

    private final String id;
    private final Date startedAt;
    private final User user;
    private AtomicBoolean autoCaptured;

    static Session copySession(Session session) {
        Session copy = new Session(session.id, session.startedAt,
            session.user, session.unhandledCount.get(), session.handledCount.get());
        copy.tracked.set(session.tracked.get());
        copy.autoCaptured.set(session.isAutoCaptured());
        return copy;
    }

    public Session(String id, Date startedAt, User user, boolean autoCaptured) {
        this.id = id;
        this.startedAt = new Date(startedAt.getTime());
        this.user = user;
        this.autoCaptured = new AtomicBoolean(autoCaptured);
    }

    Session(String id, Date startedAt, User user, int unhandledCount, int handledCount) {
        this.id = id;
        this.startedAt = new Date(startedAt.getTime());
        this.user = user;
        this.autoCaptured = new AtomicBoolean(false);
        this.unhandledCount = new AtomicInteger(unhandledCount);
        this.handledCount = new AtomicInteger(handledCount);
        this.tracked = new AtomicBoolean(true);
    }

    private AtomicInteger unhandledCount = new AtomicInteger();
    private AtomicInteger handledCount = new AtomicInteger();
    private AtomicBoolean tracked = new AtomicBoolean(false);
    final AtomicBoolean isStopped = new AtomicBoolean(false);

    String getId() {
        return id;
    }

    Date getStartedAt() {
        return new Date(startedAt.getTime());
    }

    User getUser() {
        return user;
    }

    int getUnhandledCount() {
        return unhandledCount.intValue();
    }

    int getHandledCount() {
        return handledCount.intValue();
    }

    Session incrementHandledAndCopy() {
        handledCount.incrementAndGet();
        return copySession(this);
    }

    Session incrementUnhandledAndCopy() {
        unhandledCount.incrementAndGet();
        return copySession(this);
    }

    AtomicBoolean isTracked() {
        return tracked;
    }

    boolean isAutoCaptured() {
        return autoCaptured.get();
    }

    void setAutoCaptured(boolean autoCaptured) {
        this.autoCaptured.set(autoCaptured);
    }

    @Override
    public void toStream(@NonNull JsonStream writer) throws IOException {
        writer.beginObject()
            .name("id").value(id)
            .name("startedAt").value(DateUtils.toIso8601(startedAt));

        if (user != null) {
            writer.name("user").value(user);
        }
        writer.endObject();
    }
}
