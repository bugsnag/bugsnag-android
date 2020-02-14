package com.bugsnag.android;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public final class Session implements JsonStream.Streamable, UserAware {

    private final File file;
    private String id;
    private Date startedAt;
    private User user;
    private App app;
    private Device device;

    private final AtomicBoolean autoCaptured = new AtomicBoolean(false);
    private final AtomicInteger unhandledCount = new AtomicInteger();
    private final AtomicInteger handledCount = new AtomicInteger();
    private final AtomicBoolean tracked = new AtomicBoolean(false);
    final AtomicBoolean isPaused = new AtomicBoolean(false);

    static Session copySession(Session session) {
        Session copy = new Session(session.id, session.startedAt,
                session.user, session.unhandledCount.get(), session.handledCount.get());
        copy.tracked.set(session.tracked.get());
        copy.autoCaptured.set(session.isAutoCaptured());
        return copy;
    }

    Session(String id, Date startedAt, User user, boolean autoCaptured) {
        this.id = id;
        this.startedAt = new Date(startedAt.getTime());
        this.user = user;
        this.autoCaptured.set(autoCaptured);
        this.file = null;
    }

    Session(String id, Date startedAt, User user, int unhandledCount, int handledCount) {
        this(id, startedAt, user, false);
        this.unhandledCount.set(unhandledCount);
        this.handledCount.set(handledCount);
        this.tracked.set(true);
    }

    Session(File file) {
        this.file = file;
    }

    @NonNull
    public String getId() {
        return id;
    }

    public void setId(@NonNull String id) {
        this.id = id;
    }

    @NonNull
    public Date getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(@NonNull Date startedAt) {
        this.startedAt = startedAt;
    }

    @NonNull
    @Override
    public User getUser() {
        return user;
    }

    @Override
    public void setUser(@Nullable String id, @Nullable String email, @Nullable String name) {
        user = new User(id, email, name);
    }

    void setApp(App app) {
        this.app = app;
    }

    void setDevice(Device device) {
        this.device = device;
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
        if (file != null) {
            writer.value(file);
        } else {
            writer.beginObject();
            writer.name("notifier").value(Notifier.INSTANCE);
            writer.name("app").value(app);
            writer.name("device").value(device);
            writer.name("sessions").beginArray();
            serializeSession(writer);
            writer.endArray();
            writer.endObject();
        }
    }

    private void serializeSession(@NonNull JsonStream writer) throws IOException {
        writer.beginObject();
        writer.name("id").value(id);
        writer.name("startedAt").value(DateUtils.toIso8601(startedAt));
        writer.name("user").value(user);
        writer.endObject();
    }
}
