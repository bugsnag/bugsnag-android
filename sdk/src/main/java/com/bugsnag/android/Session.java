package com.bugsnag.android;

import java.util.Date;

class Session {

    private String id;
    private Date startedAt;
    private User user;
    private int unhandledCount;
    private int handledCount;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Date getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Date startedAt) {
        this.startedAt = startedAt;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public int getUnhandledCount() {
        return unhandledCount;
    }

    public void setUnhandledCount(int unhandledCount) {
        this.unhandledCount = unhandledCount;
    }

    public int getHandledCount() {
        return handledCount;
    }

    public void setHandledCount(int handledCount) {
        this.handledCount = handledCount;
    }

    public void incrementHandledErrCount() {
        handledCount++;
    }

    public void incrementUnhandledErrCount() {
        unhandledCount++;
    }
}
