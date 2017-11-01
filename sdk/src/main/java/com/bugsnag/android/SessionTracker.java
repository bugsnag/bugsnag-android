package com.bugsnag.android;

import android.support.annotation.Nullable;

import java.util.Date;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

class SessionTracker {

    private Session currentSession;
    private final Queue<Session> sessionQueue = new ConcurrentLinkedQueue<>();

    @Nullable Session getCurrentSession() {
        return currentSession;
    }

    void startNewSession(Date date, User user) {
        Session session = new Session();
        session.setId(UUID.randomUUID().toString());
        session.setStartedAt(date);
        session.setUser(user);

        // TODO handle sending/storing sessions here!
        sessionQueue.add(session); // store previous session
        currentSession = session;
    }

}
