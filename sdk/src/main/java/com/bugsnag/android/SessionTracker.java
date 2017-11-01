package com.bugsnag.android;

import android.support.annotation.Nullable;

import java.util.Date;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

class SessionTracker {

    private final Object lock = new Object();
    private final Queue<Session> sessionQueue = new ConcurrentLinkedQueue<>();
    private Session currentSession;

    void startNewSession(Date date, User user) {
        synchronized (lock) {
            Session session = new Session();
            session.setId(UUID.randomUUID().toString());
            session.setStartedAt(date);
            session.setUser(user);

            // TODO handle sending/storing sessions here!
            sessionQueue.add(session); // store previous session
            currentSession = session;
        }
    }

    @Nullable Session getCurrentSession() {
        synchronized (lock) {
            return currentSession;
        }
    }

    void incrementUnhandledError() {
        synchronized (lock) {
            if (currentSession != null) {
                currentSession.incrementUnhandledErrCount();
            }
        }
    }

    void incrementHandledError() {
        synchronized (lock) {
            if (currentSession != null) {
                currentSession.incrementHandledErrCount();
            }
        }
    }

}
