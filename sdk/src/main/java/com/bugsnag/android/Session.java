package com.bugsnag.android;

import java.util.Date;

class Session {

    private String id;
    private Date startedAt;
    private User user;
    private int unhandledCount;
    private int handledCount;

    String getId() {
        return id;
    }

    void setId(String id) {
        this.id = id;
    }

    Date getStartedAt() {
        return startedAt;
    }

    void setStartedAt(Date startedAt) {
        this.startedAt = startedAt;
    }

    User getUser() {
        return user;
    }

    void setUser(User user) {
        this.user = user;
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
}
