package com.bugsnag.android;

import android.content.Context;

import java.util.ArrayList;
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
    }

    /**
     * Attempts to send any tracked sessions to the API, and store in the event of failure
     */
    synchronized void send() {
        List<Session> sessions = new ArrayList<>();
        sessions.addAll(sessionTracker.sessionQueue);
        sessionTracker.sessionQueue.clear();

        AppData appData = new AppData(context, config, sessionTracker);
        SessionTrackingPayload payload = new SessionTrackingPayload(sessions, appData);

        // TODO endpoint
        String endpoint = "";

        try {
            apiClient.postSessionTrackingPayload(endpoint, payload, config.getSessionApiHeaders());
        } catch (NetworkException e) { // store for later sending
            store(payload);
        } catch (BadResponseException e) { // drop bad data
            Logger.warn("Invalid session tracking payload", e);
        }
    }

    /**
     * Persists any sessions which haven't been sent to the API yet
     */
    synchronized void store(SessionTrackingPayload payload) {
        sessionStore.write(payload);
        // TODO handle read + send of payload
    }

}
