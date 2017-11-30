package com.bugsnag.android;

import java.util.Date;

class Session {

    private final String id;
    private final Date startedAt;
    private final User user;

    public Session(String id, Date startedAt, User user) {
        this.id = id;
        this.startedAt = startedAt;
        this.user = user;
    }

    private int unhandledCount;
    private int handledCount;
    private transient boolean autoCaptured;

    String getId() {
        return id;
    }

    Date getStartedAt() {
        return startedAt;
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
}
