package com.bugsnag.android;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.File;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

class SessionTracker implements Application.ActivityLifecycleCallbacks {

    private static final String KEY_LIFECYCLE_CALLBACK = "ActivityLifecycle";
    private static final int DEFAULT_TIMEOUT_MS = 30000;

    private final ConcurrentHashMap<String, Boolean> foregroundActivities = new ConcurrentHashMap<>();
    private final Configuration configuration;
    private final long timeoutMs;
    private final Client client;
    private final SessionStore sessionStore;
    private final SessionTrackingApiClient apiClient;
    private final String endpoint;

    // This most recent time an Activity was stopped.
    private AtomicLong activityLastStoppedAtMs = new AtomicLong(0);

    // The first Activity in this 'session' was started at this time.
    private AtomicLong activityFirstStartedAtMs = new AtomicLong(0);
    private AtomicReference<Session> currentSession = new AtomicReference<>();
    private Semaphore flushingRequest = new Semaphore(1);

    SessionTracker(Configuration configuration, Client client, SessionStore sessionStore,
                   SessionTrackingApiClient apiClient) {
        this(configuration, client, DEFAULT_TIMEOUT_MS, sessionStore, apiClient);
    }

    SessionTracker(Configuration configuration, Client client, long timeoutMs,
                   SessionStore sessionStore, SessionTrackingApiClient apiClient) {
        this.configuration = configuration;
        this.client = client;
        this.timeoutMs = timeoutMs;
        this.sessionStore = sessionStore;
        this.apiClient = apiClient;
        this.endpoint = configuration.getSessionEndpoint();
    }

    /**
     * Starts a new session with the given date and user.
     * <p>
     * A session will only be created if {@link Configuration#shouldAutoCaptureSessions()} returns
     * true.
     *
     * @param date the session start date
     * @param user the session user (if any)
     */
    void startNewSession(@NonNull Date date, @Nullable User user, boolean autoCaptured) {
        Session session = new Session(UUID.randomUUID().toString(), date, user, autoCaptured);
        currentSession.set(session);
        trackSessionIfNeeded(session);
    }

    /**
     * Determines whether or not a session should be tracked. If this is true, the session will be
     * stored and sent to the Bugsnag API, otherwise no action will occur in this method.
     *
     * @param session      the session
     */
    private void trackSessionIfNeeded(final Session session) {
        boolean notifyForRelease = configuration.shouldNotifyForReleaseStage(getReleaseStage());

        if (notifyForRelease &&
            (configuration.shouldAutoCaptureSessions() || !session.isAutoCaptured()) &&
            session.isTracked().compareAndSet(false, true)) {
            try {
                Async.run(new Runnable() {
                    @Override
                    public void run() {
                    //TODO:SM It would be good to optimise this
                    flushStoredSessions();

                    SessionTrackingPayload payload = new SessionTrackingPayload(session, client.appData);

                    try {
                        apiClient.postSessionTrackingPayload(endpoint, payload, configuration.getSessionApiHeaders());
                    } catch (NetworkException e) { // store for later sending
                        Logger.info("Failed to post session payload");
                        sessionStore.write(session);
                    } catch (BadResponseException e) { // drop bad data
                        Logger.warn("Invalid session tracking payload", e);
                    }
                    }
                });
            } catch (RejectedExecutionException e) {
                // This is on the current thread but there isn't much else we can do
                sessionStore.write(session);
            }
        }
    }

    /**
     * Track a new session when auto capture is enabled via config after initialisation.
     */
    void onAutoCaptureEnabled() {
        Session session = currentSession.get();
        if (session != null && !foregroundActivities.isEmpty()) {
            // If there is no session we will wait for one to be created
            trackSessionIfNeeded(session);
        }
    }

    private String getReleaseStage() {
        return client.appData.getReleaseStage();
    }

    @Nullable
    Session getCurrentSession() {
        return currentSession.get();
    }

    void incrementUnhandledError() {
        Session session = currentSession.get();
        if (session != null) {
            session.incrementUnhandledErrCount();
        }
    }

    void incrementHandledError() {
        Session session = currentSession.get();
        if (session != null) {
            session.incrementHandledErrCount();
        }
    }

    /**
     * Attempts to flush session payloads stored on disk
     */
    void flushStoredSessions() {
        if (flushingRequest.tryAcquire(1)) {
            try {
                List<File> storedFiles;

                storedFiles = sessionStore.findStoredFiles();

                if (!storedFiles.isEmpty()) {
                    SessionTrackingPayload payload = new SessionTrackingPayload(storedFiles, client.appData);

                    //TODO:SM Reduce duplication here and above
                    try {
                        apiClient.postSessionTrackingPayload(endpoint, payload, configuration.getSessionApiHeaders());
                        deleteStoredFiles(storedFiles);
                    } catch (NetworkException e) { // store for later sending
                        Logger.info("Failed to post stored session payload");
                    } catch (BadResponseException e) { // drop bad data
                        Logger.warn("Invalid session tracking payload", e);
                        deleteStoredFiles(storedFiles);
                    }
                }
            } finally {
                flushingRequest.release(1);
            }
        }
    }

    private void deleteStoredFiles(Collection<File> storedFiles) {
        for (File storedFile : storedFiles) {
            storedFile.delete();
        }
    }

    @Override
    public void onActivityCreated(@NonNull Activity activity, Bundle savedInstanceState) {
        leaveLifecycleBreadcrumb(getActivityName(activity), "onCreate()");
    }

    @Override
    public void onActivityStarted(@NonNull Activity activity) {
        String activityName = getActivityName(activity);
        leaveLifecycleBreadcrumb(activityName, "onStart()");
        updateForegroundTracker(activityName, true, System.currentTimeMillis());
    }

    @Override
    public void onActivityResumed(@NonNull Activity activity) {
        leaveLifecycleBreadcrumb(getActivityName(activity), "onResume()");
    }

    @Override
    public void onActivityPaused(@NonNull Activity activity) {
        leaveLifecycleBreadcrumb(getActivityName(activity), "onPause()");
    }

    @Override
    public void onActivityStopped(@NonNull Activity activity) {
        String activityName = getActivityName(activity);
        leaveLifecycleBreadcrumb(activityName, "onStop()");
        updateForegroundTracker(activityName, false, System.currentTimeMillis());
    }

    @Override
    public void onActivitySaveInstanceState(@NonNull Activity activity, Bundle outState) {
        leaveLifecycleBreadcrumb(getActivityName(activity), "onSaveInstanceState()");
    }

    @Override
    public void onActivityDestroyed(@NonNull Activity activity) {
        leaveLifecycleBreadcrumb(getActivityName(activity), "onDestroy()");
    }

    private String getActivityName(@NonNull Activity activity) {
        return activity.getClass().getSimpleName();
    }

    void leaveLifecycleBreadcrumb(String activityName, String lifecycleCallback) {
        leaveBreadcrumb(activityName, lifecycleCallback);
    }

    private void leaveBreadcrumb(String activityName, String lifecycleCallback) {
        if (configuration.isAutomaticallyCollectingBreadcrumbs()) {
            Map<String, String> metadata = new HashMap<>();
            metadata.put(KEY_LIFECYCLE_CALLBACK, lifecycleCallback);
            client.leaveBreadcrumb(activityName, BreadcrumbType.NAVIGATION, metadata);
        }
    }

    /**
     * Tracks whether an activity is in the foreground or not.
     * <p>
     * If an activity leaves the foreground, a timeout should be recorded (e.g. 30s), during which
     * no new sessions should be automatically started.
     * <p>
     * If an activity comes to the foreground and is the only foreground activity, a new session
     * should be started, unless the app is within a timeout period.
     *
     * @param activityName the activity name
     * @param activityStarting whether the activity is being started or not
     * @param nowMs The current time in ms
     */
    void updateForegroundTracker(String activityName, boolean activityStarting, long nowMs) {
        if (activityStarting) {
            long noActivityRunningForMs = nowMs - activityLastStoppedAtMs.get();

            //TODO:SM Race condition between isEmpty and put
            if (foregroundActivities.isEmpty() &&
                noActivityRunningForMs >= timeoutMs &&
                configuration.shouldAutoCaptureSessions()) {

                activityFirstStartedAtMs.set(nowMs);
                startNewSession(new Date(nowMs), client.user, true);
            }
            foregroundActivities.put(activityName, true);
        } else {
            foregroundActivities.remove(activityName);
            activityLastStoppedAtMs.set(nowMs);
        }
    }

    boolean isInForeground() {
        return !foregroundActivities.isEmpty();
    }

    //TODO:SM This shouldnt be here
    long getDurationInForegroundMs(long nowMs) {
        long durationMs = 0;
        long sessionStartTimeMs = activityFirstStartedAtMs.get();

        if (isInForeground() && sessionStartTimeMs != 0) {
            durationMs = nowMs - sessionStartTimeMs;
        }
        return durationMs > 0 ? durationMs : 0;
    }

}
