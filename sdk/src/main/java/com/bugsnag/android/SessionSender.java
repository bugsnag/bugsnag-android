package com.bugsnag.android;

import android.content.Context;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Coordinates sending/storing of tracked sessions
 */
class SessionSender {

    private final SessionTracker sessionTracker;
    private final SessionStore sessionStore;
    private final SessionTrackingApiClient apiClient;
    private final Context context;
    private final Configuration config;
    private final String endpoint;

    SessionSender(SessionTracker sessionTracker,
                  SessionStore sessionStore,
                  SessionTrackingApiClient apiClient,
                  Context context,
                  Configuration configuration) {
        this.sessionTracker = sessionTracker;
        this.sessionStore = sessionStore;
        this.apiClient = apiClient;
        this.context = context;
        this.config = configuration;
        this.endpoint = config.getSessionEndpoint();
    }

    /**
     * Attempts to send all sessions (both from memory + disk)
     */
    void send() {
        AppData appData = new AppData(context, config, sessionTracker);
        SessionTrackingPayload payload = new SessionTrackingPayload(getPendingSessions(), appData);
        send(payload);
        flushStoredSessions();
    }

    void storeAllSessions() {
        Collection<Session> sessions = getPendingSessions();

        if (!sessions.isEmpty()) {
            for (Session session : sessions) {
                sessionStore.write(session);
            }
        }
    }

    private Collection<Session> getPendingSessions() {
        List<Session> sessions = new ArrayList<>();
        sessions.addAll(sessionTracker.sessionQueue);
        sessionTracker.sessionQueue.clear();
        return sessions;
    }

    /**
     * Attempts to flush session payloads stored on disk
     */
    private synchronized void flushStoredSessions() {
        List<File> storedFiles = sessionStore.findStoredFiles();
        AppData appData = new AppData(context, config, sessionTracker);
        SessionTrackingPayload payload = new SessionTrackingPayload(storedFiles, appData);

        try {
            apiClient.postSessionTrackingPayload(endpoint, payload, config.getSessionApiHeaders());

            deleteStoredFiles(storedFiles);
        } catch (NetworkException e) { // store for later sending
            Logger.info("Failed to post stored session payload");
        } catch (BadResponseException e) { // drop bad data
            Logger.warn("Invalid session tracking payload", e);
            deleteStoredFiles(storedFiles);
        }
    }

    private void deleteStoredFiles(Collection<File> storedFiles) {
        for (File storedFile : storedFiles) {
            storedFile.delete();
        }
    }

    /**
     * Attempts to send any tracked sessions to the API, and store in the event of failure
     */
    private synchronized void send(SessionTrackingPayload payload) {
        try {
            apiClient.postSessionTrackingPayload(endpoint, payload, config.getSessionApiHeaders());
        } catch (NetworkException e) { // store for later sending
            Logger.info("Failed to post session payload, storing on disk");

            for (Session session : payload.getSessions()) {
                sessionStore.write(session);
            }
        } catch (BadResponseException e) { // drop bad data
            Logger.warn("Invalid session tracking payload", e);
        }
    }

}
